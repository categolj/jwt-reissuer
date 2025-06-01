# JWTReissuer

JWTReissuer is an API server that validates JWTs issued by internal systems (like Kubernetes API
Server) and re-signs them with your own private key, making them suitable for use in public
services (like AWS STS).


## IRSA (IAM Roles for Service Accounts) Setup in Any Kubernetes Cluster

Deploy the JWT Reissuer with the following configuration:

```yaml
      - name: jwt-reissuer
        image: ghcr.io/categolj/jwt-reissuer:native
        env:
        - name: reissuer.jwt.public-key
          value: file:/etc/keys/pub.pem
        - name: reissuer.jwt.private-key
          value: file:/etc/keys/key.pem
        - name: reissuer.oidc.issuer-uri
          value: https://kubernetes.default.svc.cluster.local
        - name: reissuer.oidc.bearer-token
          value: file:/var/run/secrets/kubernetes.io/serviceaccount/token
        - name: reissuer.oidc.client-bundle-name
          value: k8s
        - name: spring.ssl.bundle.pem.k8s.truststore.certificate
          value: file:/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        # ...
        volumeMounts:
        - name: key-volume
          mountPath: /etc/keys
          readOnly: true
      volumes:
      - name: key-volume
        secret:
          secretName: rsa-key1
```

and

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: rsa-key1
  namespace: your-namespace
type: Opaque
stringData:
  pub.pem: |
    -----BEGIN PUBLIC KEY-----
    ...
    -----END PUBLIC KEY-----
  key.pem: |
    -----BEGIN PRIVATE KEY-----
    ...
    -----END PRIVATE KEY-----
```

You should also need an HTTPS ingress to access the JWT Reissuer API.


Create an OpenID Connect (OIDC) provider in AWS IAM:

```bash
JWT_REISSUER_DOMAIN=jwt-reissuer.<your_domain>
FINGERPRINT=$(openssl s_client -servername ${JWT_REISSUER_DOMAIN} -showcerts -connect ${JWT_REISSUER_DOMAIN}:443 </dev/null 2>/dev/null | openssl x509 -fingerprint -sha1 -noout | sed 's/sha1 Fingerprint=//' | sed 's/://g')
```

```bash
cat <<EOF > oidc-provider.json
{
    "Url": "https://${JWT_REISSUER_DOMAIN}",
    "ClientIDList": [
        "sts.amazonaws.com"
    ],
    "ThumbprintList": [
        "${FINGERPRINT}"
    ]
}
EOF

aws iam create-open-id-connect-provider --cli-input-json file://oidc-provider.json
```

To find the ARN of the OIDC provider, you can use the following command:

```bash
OIDC_PROVIDER_ARN=$(aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[?ends_with(Arn, '${JWT_REISSUER_DOMAIN}')].Arn" --output text)
```

Create a trust policy for the IAM role that will be used by the Kubernetes service account:

```bash
NAMESPACE=default
SERVICE_ACCOUNT_NAME=aws-cli
cat << EOF > k8s-${NAMESPACE}-${SERVICE_ACCOUNT_NAME}-trust-policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Federated": "${OIDC_PROVIDER_ARN}"
            },
            "Action": "sts:AssumeRoleWithWebIdentity",
            "Condition": {
                "StringLike": {
                    "${JWT_REISSUER_DOMAIN}:sub": "system:serviceaccount:${NAMESPACE}:${SERVICE_ACCOUNT_NAME}",
                    "${JWT_REISSUER_DOMAIN}:aud": "sts.amazonaws.com"
                }
            }
        }
    ]
}
EOF

aws iam create-role --role-name k8s-${NAMESPACE}-${SERVICE_ACCOUNT_NAME} --assume-role-policy-document file://k8s-${NAMESPACE}-${SERVICE_ACCOUNT_NAME}-trust-policy.json
```

For the role to have permissions to access S3 buckets with a specific prefix, you can create a policy like this:

```bash
export BUCKET_PREFIX=k8s-${NAMESPACE}-${SERVICE_ACCOUNT_NAME}

cat <<EOF > s3-prefix-full-access-${BUCKET_PREFIX}-policy.json
{
  "Version": "2012-10-17",
  "Statement": [

    {
      "Sid": "FullAccessTo$(echo ${BUCKET_PREFIX} | sed 's/\-//g')Buckets",
      "Effect": "Allow",
      "Action": [
        "s3:*"
      ],
      "Resource": [
        "arn:aws:s3:::${BUCKET_PREFIX}-*",
        "arn:aws:s3:::${BUCKET_PREFIX}-*/*"
      ]
    },
    {
      "Sid": "ListBucketsForConsoleAccess",
      "Effect": "Allow",
      "Action": [
        "s3:ListAllMyBuckets",
        "s3:GetBucketLocation"
      ],
      "Resource": "*"
    }
  ]
}
EOF

aws iam put-role-policy --role-name k8s-${NAMESPACE}-${SERVICE_ACCOUNT_NAME} --policy-name s3-prefix-full-access-${BUCKET_PREFIX} --policy-document file://s3-prefix-full-access-${BUCKET_PREFIX}-policy.json
```

Let's use the AWS CLI to create a Kubernetes service account that uses the IAM role we just created:

```bash

```yaml
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export AWS_REGION=ap-northeast-1
cat <<EOF > aws-cli.yaml
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: aws-cli
  namespace: default
---
apiVersion: v1
kind: Pod
metadata:
  name: aws-cli
  namespace: default
spec:
  serviceAccountName: aws-cli
  volumes:
  - name: shared-token
    emptyDir: {}
  containers:
  - name: aws-cli
    image: public.ecr.aws/aws-cli/aws-cli
    command: [ "sleep", "infinity" ]
    env:
    - name: AWS_REGION
      value: ${AWS_REGION}
    - name: AWS_ROLE_ARN
      value: arn:aws:iam::${AWS_ACCOUNT_ID}:role/k8s-default-aws-cli
    - name: AWS_WEB_IDENTITY_TOKEN_FILE
      value: /tmp/token
    - name: AWS_ROLE_SESSION_NAME
      value: k8s-demo
    volumeMounts:
    - name: shared-token
      mountPath: /tmp
  - name: token-refresher
    image: curlimages/curl:latest
    command: ["/bin/sh"]
    args:
    - -c
    - |
      while true; do
        echo "\$(date): Refreshing token..."
        curl -s --fail -XPOST https://${JWT_REISSUER_DOMAIN}/token -H "Authorization: Bearer \$(cat /run/secrets/kubernetes.io/serviceaccount/token)" -o /tmp/token
        if [ \$? -eq 0 ]; then
          echo "\$(date): Token refreshed successfully"
        else
          echo "\$(date): Failed to refresh token"
        fi
        sleep 7200 # Sleep for 2 hours before refreshing again
      done
    env:
    - name: JWT_REISSUER_DOMAIN
      value: ${JWT_REISSUER_DOMAIN}
    volumeMounts:
    - name: shared-token
      mountPath: /tmp
  restartPolicy: Never
---
EOF

kubectl apply -f aws-cli.yaml
```

Now, you can access the AWS CLI in the pod:

```bash
kubectl exec -ti aws-cli -c aws-cli -- bash
```

Inside the pod, you can verify that the AWS CLI is configured correctly and that you can access S3 buckets:

You should see the AWS account ID and other details of the IAM role.

```bash
aws sts get-caller-identity
```

Then you can create a bucket with the prefix `k8s-default-aws-cli-demo`:

```bash
aws s3 mb s3://k8s-default-aws-cli-demo
```

You should see a successful message like this:

```
make_bucket: k8s-default-aws-cli-demo
```

But if you try to create a bucket with a different prefix, you will get an error:

```bash
aws s3 mb s3://k9s-default-aws-cli-demo
```

```
make_bucket failed: s3://k9s-default-aws-cli-demo An error occurred (AccessDenied) when calling the CreateBucket operation: User: arn:aws:sts::xxxxxxxxxxxx:assumed-role/k8s-default-aws-cli/k8s-demo is not authorized to perform: s3:CreateBucket on resource: "arn:aws:s3:::k9s-default-aws-cli-demo" because no identity-based policy allows the s3:CreateBucket action
```

You can also delete the bucket you created:

```bash
aws s3 rb s3://k8s-default-aws-cli-demo
```