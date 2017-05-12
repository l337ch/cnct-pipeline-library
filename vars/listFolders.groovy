#!/usr/bin/groovy
def call(path) {  
  def dirs = []
  if (fileExists(path)) {
    def dirNames = sh(returnStdout: true, script: "ls -dm -- ${path}/*/ | tr -d ' \n'").trim().split(',')

    for (i = 0; i < dirNames.size(); i++) {
      dirs << "${dirNames[i][0..-2]}"
    }
  }

  return dirs
}