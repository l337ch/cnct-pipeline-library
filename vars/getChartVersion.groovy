#!/usr/bin/groovy
def call(chartName) {  
  return getChartVersion(chartName)
}

def getChartVersion(chartName) {
  return sh(returnStdout: true, script: "helm inspect chart ${chartName} | grep version | awk '{ print \$2 }'").trim()
}