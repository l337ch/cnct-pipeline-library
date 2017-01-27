#!/usr/bin/groovy
def call(path) {  
  def dirs = []
  def dirNames = sh(returnStdout: true, script: "ls -dm * | tr -d ' '").trim().split(',')

  for (i = 0; i < dirNames.size(); i++) {
    dirs << "${path}/${dirNames[i]}"
  }
  return dirs
}