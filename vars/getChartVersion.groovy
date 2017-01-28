#!/usr/bin/groovy
def call(chartName) {  
  return getChartVersion(chartName)
}

def getChartVersion(chartName) {
  if (sh(returnStatus: true, script: "helm inspect chart ${chartName}") != 0) {
    return null
  } else {
    return sh(returnStdout: true, script: "helm inspect chart ${chartName} | grep version | awk '{ print \$2 }'").trim()
  }
}