# JWTReissuer

JWTReissuer is an API server that validates JWTs issued by internal systems (like Kubernetes API
Server) and re-signs them with your own private key, making them suitable for use in public
services (like AWS STS).
