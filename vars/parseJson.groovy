#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import groovy.json.JsonSlurper

@NonCPS
def parseJsonText(String jsonText) {
  final slurper = new JsonSlurper()
  return new HashMap<>(slurper.parseText(jsonText))
}