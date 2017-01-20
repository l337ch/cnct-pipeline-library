#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return parseJson(config.json)
}

@NonCPS
def parseJson(jsonText) {
  final jsonSlurper = new JsonSlurper()
  return new HashMap<>(jsonSlurper.parseText(jsonText))
}