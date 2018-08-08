def call(String arg) {
  configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]) {
    sh 'mvn -B -s ${MAVEN_SETTINGS} $arg'
  }
}
