#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import groovy.io.FileType

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return parseJson(config.path)
}

@NonCPS
def getDockerfileFolders(path) {
  def dirs = []
  new File(path).currentDir.eachFile FileType.DIRECTORIES, {
      dirs << it.name
  }

  return dirs
}