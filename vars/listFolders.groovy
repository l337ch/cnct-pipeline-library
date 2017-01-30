#!/usr/bin/groovy
def call(path) {  
  def dirs = []
  def dirNames = sh(returnStdout: true, script: "ls -dm -- ${path}/*/ | tr -d ' '").trim().split(',')

  for (i = 0; i < dirNames.size(); i++) {
    dirs << "${dirNames[i][0..-2]}"
  }
  return dirs
}