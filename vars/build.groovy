import com.samyaksolution.utils.checkoutSCM
import com.samyaksolution.utils.buildCompileApp
import com.samyaksolution.utils.uploadArtifacts
import com.samyaksolution.utils.buildUploadImages
import com.samyaksolution.utils.loginArtifactManagement
import com.samyaksolution.utils.*
def call(body) {
    def BUILD_COMMAND
    def PROFILE
    def ACCOUNT_ID
    pipeline {
        agent {
            docker {
                label 'LXSSSIT01'
                image '471112706924.dkr.ecr.us-east-1.amazonaws.com/samyaksolution/node-java-builder:latest'
                args '''
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    -v /var/lib/jenkins/.aws:/home/jenkins/.aws \
                    --user root \
                    --privileged \
                    --memory=6g \
                    --cpus=2
                '''
            }
        }
        environment {
            NODE_OPTIONS = '--max-old-space-size=1500'
        }
        options {
            timestamps()
            buildDiscarder(logRotator(numToKeepStr: '20'))
        }
        stages {
            stage('Verify Tools') {
                steps {
                    script {
                        println("Verifying tools: Node.js, npm, git")
                        sh 'node -v'
                        sh 'npm -v'
                        sh 'git --version'
                        sh 'jq --version'
                    }
                }
            }
            stage ("Checkout SCM") {
                steps{
                    script {
                        println("Checkout SCM for ${env.GIT_URL} on branch ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}")
                        new checkoutSCM().call(env.GIT_URL, env.GIT_BRANCH)
                    }
                }
            }
            stage ("Build") {
                steps {
                    script {
                        println("Building the project")
                        new buildCompileApp().call()
                    }
                }
            }
            stage ("Upload Artifacts") {
                steps {
                    script {
                        println("Uploading Artifacts")
                        //new loginArtifactManagement().call()
                        //new uploadArtifacts().call()
                    }
                }
            }
            stage("Build and Upload Image") {
                steps {
                    script {
                        println("Build the image and push to ECR")
                        new buildUploadImages().call()
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
}