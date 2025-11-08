package com.samyaksolution.utils
def call(def GIT_URL, def GIT_BRANCH) {
    
    checkout scm: [
        $class: 'GitSCM',
        branches: [[name: GIT_BRANCH]],
        userRemoteConfigs: [[url: GIT_URL, credentialsId: 'minaxi-esamyak-pat']],
        extensions: [[$class: 'CleanBeforeCheckout']]
    ]
    sh "ls -lart"
}