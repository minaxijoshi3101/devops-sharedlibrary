package com.samyaksolution.utils

def call() {
    def props = readProperties file: 'app.properties'
    props.each { key, value -> env."${key}" = value }

    def account_id = env.ACCOUNT_ID
    def region = env.REGION ?: "us-east-1"
    def ecr_repo_name = env.ECR_REPO_NAME ?: "samyaksolution"

    println("APP_TYPE: ${env.APP_TYPE}")

    // Jenkins environment variables for AWS credentials
    withCredentials([[ 
        $class: 'UsernamePasswordMultiBinding',
        credentialsId: 'aws-credentials', 
        usernameVariable: 'AWS_ACCESS_KEY_ID', 
        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {

        def app_name
        def version

        if (env.APP_TYPE == "nodejs") {
            app_name = sh(script: "jq -r '.name' package.json", returnStdout: true).trim()
            version  = sh(script: "jq -r '.version' package.json", returnStdout: true).trim()
        }
        else if (env.APP_TYPE == "java") {
            app_name = sh(script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"artifactId\"]/text()' pom.xml", returnStdout: true).trim()
            version  = sh(script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"version\"]/text()' pom.xml", returnStdout: true).trim()
        } else {
            error "Unsupported APP_TYPE: ${env.APP_TYPE}"
        }

        println("appname and version are: ${app_name} ${version}")

        // Replace placeholders in Dockerfile
        sh """
            set -x
            sed -i "s;%APP_NAME%;${app_name};g" Dockerfile
            sed -i "s;%VERSION%;${version};g" Dockerfile
            cat Dockerfile
            # Login to ECR
            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.${region}.amazonaws.com

            # Build Docker image
            docker build -t ${ecr_repo_name}/${app_name}:${version} .
            docker tag ${ecr_repo_name}/${app_name}:${version} ${account_id}.dkr.ecr.${region}.amazonaws.com/${ecr_repo_name}/${app_name}:${version}
            docker tag ${ecr_repo_name}/${app_name}:${version} ${account_id}.dkr.ecr.${region}.amazonaws.com/${ecr_repo_name}/${app_name}:latest

            # Uncomment to push
            docker push ${account_id}.dkr.ecr.${region}.amazonaws.com/${ecr_repo_name}/${app_name}:${version}
            docker push ${account_id}.dkr.ecr.${region}.amazonaws.com/${ecr_repo_name}/${app_name}:latest

            docker images | grep ${app_name} || true
        """

        println("Docker image ${app_name}:${version} built and tagged successfully")
    }
}
