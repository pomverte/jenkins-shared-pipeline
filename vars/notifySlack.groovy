// https://api.slack.com/docs/messages/builder
def call(String buildStatus, String[] channels = []) {
  withCredentials([string(credentialsId: 'SLACK_WEBHOOK_API_TOKEN_ID', variable: 'SLACK_API_TOKEN')]) {
    def jobNameDecoded = URLDecoder.decode("${env.JOB_NAME}", "UTF-8")
    def jobNameSplitted = jobNameDecoded.split('/')
    def jobNameShort = jobNameSplitted.size() > 1 ? jobNameSplitted[jobNameSplitted.size() - 2] : jobNameDecoded
    def author = gitCommitAuthorName()
    def colorCode = (buildStatus == 'SUCCESS') ? '#36a64f' : '#e9a820'
    channels += "#jenkins"
    channels.each {
      sh "curl -X POST -H 'Content-type: application/json' --data '{\"username\":\"Jenkins\",\"icon_url\":\"https://www.build-business-websites.co.uk/resources/jenkins-butler-square.png\",\"channel\":\"${it}\",\"attachments\":[{\"fallback\":\"${currentBuild.absoluteUrl}\",\"color\":\"$colorCode\",\"author_name\":\"$author\",\"title\":\"$jobNameShort\",\"title_link\":\"${env.BUILD_URL}console\",\"text\":\"Build ${env.JOB_NAME} #${env.BUILD_NUMBER} is a ${buildStatus}\"}]}' https://hooks.slack.com/services/${SLACK_API_TOKEN}"
    }
  }
}
