#!/usr/bin/groovy
def call() {  
  return getGitSHA()
}

def getGitSHA() {
  return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}