@Library('pipeline-library') _
import com.samyaksolution.utils.*
import com.samyaksolution.utils.deployApp
pipeline {    
    agent {
        docker {
            label 'LXSSSIT01'
            image '471112706924.dkr.ecr.us-east-1.amazonaws.com/samyaksolution/node-java-builder:latest'
            args '-v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.aws:/home/jenkins/.aws --user root --privileged'
        }
    }
    parameters {
        string(name: 'AGENT_LABEL', defaultValue: 'LXSSSIT01', description: 'Jenkins agent label')
    }
    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }
    stages {
        stage ("Checkout SCM") {
            steps{
                script {
                    println("Checkout SCM for ${params.REPO_NAME} on branch ${params.BRANCH_NAME} ")
                    def git_url = "https://github.com/samyaksolution/${params.REPO_NAME}.git"
                    println("BRANCH_TYPE: ${params.BRANCH_TYPE}")
                    println("BRANCH_NAME: ${params.BRANCH_NAME}")
                    def branch = params.BRANCH_TYPE+"/"+params.BRANCH_NAME
                    println("Using branch: ${branch}")
                    new checkoutSCM().call(git_url, branch)
                }
            }
        }
        stage("Initialization") {
            steps {
                script {
                    println("Read app.properties and set environment variables")
                    def props = readProperties file: 'app.properties'
                    // Iterate over each property
                    props.each { key, value ->
                        env."${key}" = value
                    }
                }
            }
        }
        stage ("Pull the image and spin up the container") {
            agent {
                docker {
                    label "${params.TARGET_SERVER}"
                    image '471112706924.dkr.ecr.us-east-1.amazonaws.com/samyaksolution/node-java-builder:latest'
                    args '-v /var/run/docker.sock:/var/run/docker.sock -v $HOME/.aws:/home/jenkins/.aws --user root --privileged'
                }
            }
            steps{
                script {
                    println("Deploy application in SIT")
                    //read agent from params and spin up container agent to deploy
                    new deployApp().call(env."${key}")
                }
            }
        }
    }

    post {
        always {
            echo 'Cleaning up...'
            cleanWs()
        }
    }
}