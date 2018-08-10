def call(String goal) {
  def params = ''
  switch(goal) {
    case ~/^feature-.*$/:
      params = '-DpushFeatures=true -DreleaseBranchVersionSuffix=RC'
      break
    case ~/^release-.*$/:
      params = '-DpushReleases=true'
      break
    case ~/^hotfix-.*$/:
      params = '-DpushHotfixes=true -DnoHotfixBuild=true -DreleaseVersion=' + getHotfixVersion()
      break
  }
  withCredentials([usernamePassword(credentialsId: 'gitea-hvle', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASSWORD')]) {
    runMavenGoal "jgitflow:${goal} -B -DallowUntracked=true -DautoVersionSubmodules=true ${params} -DnoDeploy=true -DscmCommentPrefix=[jgitflow] -Dusername=${GIT_USER} -Dpassword=${GIT_PASSWORD}"
  }
}
