aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 471112706924.dkr.ecr.us-east-1.amazonaws.com
docker build -t samyaksolution/node-java-builder .
docker tag samyaksolution/node-java-builder:latest 471112706924.dkr.ecr.us-east-1.amazonaws.com/samyaksolution/node-java-builder:latest
docker push 471112706924.dkr.ecr.us-east-1.amazonaws.com/samyaksolution/node-java-builder:latest