#!/usr/bin/groovy
def call(hostname) {  
  return getHostIp(hostname)
}

def getHostIp(hostname) {
  return sh(returnStdout: true, script: "getent hosts ${hostname} | awk '{ print \$1 }'").trim()
}