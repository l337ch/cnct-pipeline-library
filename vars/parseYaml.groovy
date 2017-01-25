#!/usr/bin/groovy
import com.cloudbees.groovy.cps.NonCPS
import org.yaml.snakeyaml.Yaml

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  
  return parseYaml(config.yaml)
}

@NonCPS
def parseYaml(yamlText) {
  def yaml = new Yaml()
  return yaml.load(yamlText)
}