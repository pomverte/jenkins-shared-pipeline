String call(String version) {
  def splitted = "${version}".split('\\.')
  if (splitted.size() < 3 || splitted.size() > 4) {
    throw new Exception("WARNING is ${version} a semantic version ? Please read https://semver.org/")
  }
  if (splitted.size() == 4) {
    // 4 digits version : increment hotfix
    splitted[3] = ++Integer.parseInt(splitted[3])
    return splitted.join('.')
  }
  // 3 digits : create a 4th digit hotfix version
  return "${version}" + '.1'
}
