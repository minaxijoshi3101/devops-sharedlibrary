package com.samyaksolution.utils

def call() {
    def props = readProperties file: 'app.properties'
    props.each { key, value -> env."${key}" = value }
    def domain = env.ECR_REPO_NAME ?: "samyaksolution"
    def app_name
    def version

    if (env.APP_TYPE == "nodejs") {
        app_name = sh(script: "jq -r '.name' package.json", returnStdout: true).trim()
        version  = sh(script: "jq -r '.version' package.json", returnStdout: true).trim()
        println "App name: ${app_name}, Version: ${version}"

        sh """
            # Replaces all occurrences of your app’s name with ${app_name}-pkg
            sed -i "s;${app_name};${app_name}-pkg;g" package.json
            sed -i 's/"private": true/"private": false/' package.json
            npm publish
            # Revert package name back
            sed -i "s;${app_name}-pkg;${app_name};g" package.json
        """

    } else if (env.APP_TYPE == "java") {
        app_name = sh(
            script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"artifactId\"]/text()' pom.xml",
            returnStdout: true
        ).trim()
        version = sh(
            script: "xmllint --xpath '/*[local-name()=\"project\"]/*[local-name()=\"version\"]/text()' pom.xml",
            returnStdout: true
        ).trim()
        println "App name: ${app_name}, Version: ${version}"

        // Use Jenkins AWS Credentials to get CodeArtifact token
        withCredentials([[
            $class: 'AmazonWebServicesCredentialsBinding',
            accessKeyVariable: 'AWS_ACCESS_KEY_ID',
            secretKeyVariable: 'AWS_SECRET_ACCESS_KEY',
            credentialsId: 'awswebsvc-creds' 
        ]]) {
            def token = sh(
                script: "aws codeartifact get-authorization-token --domain ${domain} --domain-owner 471112706924 --region ${env.region} --query authorizationToken --output text",
                returnStdout: true
            ).trim()

            withEnv(["CODEARTIFACT_AUTH_TOKEN=${token}"]) {
                sh """
                    mkdir -p ~/.m2
                    cat > ~/.m2/settings.xml <<EOF
<settings>
  <servers>
    <server>
      <id>samyaksolution-auction</id>
      <username>aws</username>
      <password>\$CODEARTIFACT_AUTH_TOKEN</password>
    </server>
  </servers>
</settings>
EOF
                    mvn deploy -s ~/.m2/settings.xml -DskipTests
                """
            }
        }

    } else {
        error "Unsupported APP_TYPE: ${env.APP_TYPE}"
    }

    println("Artifact upload completed for ${env.APP_TYPE} application")
}
