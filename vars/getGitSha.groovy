#!/usr/bin/groovy
def call(body) {  
  return getGitSHA()
}

def getGitSHA() {
  return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}