package com.samyaksolution.utils
def call(){
    println("Checkout SCM for ${env.GIT_URL} on branch ${env.GIT_BRANCH} with commit ${env.GIT_COMMIT}")
    def props = readProperties file: 'app.properties'
    props.each { key, value -> env."${key}" = value }
    echo "Build command is: ${env.BUILD_COMMAND}"
    if (env.APP_TYPE == "nodejs") {
        sh """
            set -xe
            export NODE_OPTIONS=--max-old-space-size=4096
            export CI=false
            #npm install --legacy-peer-deps --verbose
            #(while pgrep -x node > /dev/null; do ps -o pid,pcpu,pmem,rss,vsz -p \$(pgrep -x node); sleep 2; done) &
            #npm run build --verbose --log-level=debug
            ${env.BUILD_COMMAND}
        """

    } else if (env.APP_TYPE == "java") {
        sh """
            set -x
            pwd
            ls -lart
            ${env.BUILD_COMMAND}
        """
    }
    println("SCM checkout completed")
}