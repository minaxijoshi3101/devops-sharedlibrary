package com.samyaksolution.utils

def call() {
    println("Authenticating with AWS CodeArtifact...")
    def props = readProperties file: 'app.properties'
    props.each { key, value -> env."${key}" = value }

    def app_name
    def version
    def domain = env.ECR_REPO_NAME ?: "samyaksolution"

    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'awswebsvc-creds']]) {
        if (env.APP_TYPE == "nodejs") {
            app_name = sh(script: "jq -r '.name' package.json", returnStdout: true).trim()
            version  = sh(script: "jq -r '.version' package.json", returnStdout: true).trim()
            println("App name: ${app_name}, Version: ${version}")
            sh """
                set -xe
                echo "Login to CodeArtifact (npm)"
                aws sts get-caller-identity

                aws codeartifact login \
                  --tool npm \
                  --repository ${app_name} \
                  --domain ${domain} \
                  --domain-owner ${env.ACCOUNT_ID} \
                  --region ${env.REGION}
            """
        }
        else if (env.APP_TYPE == "java") {
            app_name = sh(script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"artifactId\"]/text()' pom.xml", returnStdout: true).trim()
            version  = sh(script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"version\"]/text()' pom.xml", returnStdout: true).trim()

            env.CODEARTIFACT_AUTH_TOKEN = sh(
                script: """
                    aws codeartifact get-authorization-token \
                      --domain ${domain} \
                      --domain-owner ${env.ACCOUNT_ID} \
                      --region ${env.REGION} \
                      --query authorizationToken \
                      --output text
                """,
                returnStdout: true
            ).trim()

            println("Token exported to env.CODEARTIFACT_AUTH_TOKEN for Maven")
        }
        else {
            error "Unsupported APP_TYPE: ${env.APP_TYPE}"
        }
    }
}
