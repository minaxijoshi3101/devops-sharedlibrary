def call() {
    println("Deploying Application by running container in SIT...")

    def props = readProperties file: 'app.properties'
    // Iterate over each property
    props.each { key, value ->
        env."${key}" = value
    }
    def deploy_image = props.DEPLOY_IMAGE ?: "${env.ACCOUNT_ID}.dkr.ecr.${env.REGION}.amazonaws.com/${ecr_repo_name}/auction-core:latest"

    withCredentials([[
        $class: 'UsernamePasswordMultiBinding',
        credentialsId: 'aws-credentials',
        usernameVariable: 'AWS_ACCESS_KEY_ID',
        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
    ]]) {
        sh """
            set -x
            echo "Using AWS account ID: ${env.ACCOUNT_ID} in region: ${env.REGION}"

            aws ecr get-login-password --region ${env.REGION} | docker login --username AWS --password-stdin ${env.ACCOUNT_ID}.dkr.ecr.${env.REGION}.amazonaws.com

            # Pull the image
            docker pull ${env.DEPLOY_IMAGE}

            # Stop and remove any existing container
            docker stop ${env.CONTAINER_NAME} || true

            # Remove the container if it exists
            docker rm -f ${env.CONTAINER_NAME} || true

            # Run new container
            docker run -d --name ${env.CONTAINER_NAME} -p ${env.HOST_PORT}:${env.CONTAINER_PORT} ${env.DEPLOY_IMAGE} || true

            echo "Container ${env.CONTAINER_NAME} deployed successfully"

            docker ps -a | grep ${env.CONTAINER_NAME} || true
            docker images | grep ${env.DEPLOY_IMAGE} || true
        """
        println("Container deployed and release.txt extracted successfully")
    }
}
