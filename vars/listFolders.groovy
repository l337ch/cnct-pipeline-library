#!/usr/bin/groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return getDockerfileFolders(config.path)
}

def getDockerfileFolders(path) {
  def dirs = []
  def dirNames = sh(returnStdout: true, script: "ls -dm * | tr -d ' '").trim().split(',')

  for (i = 0; i < dirNames.size(); i++) {
    dirs << "${path}/${dirNames[i]}"
  }
  return dirs
}