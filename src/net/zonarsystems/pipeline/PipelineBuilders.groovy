package net.zonarsystems.pipeline

def loadPipelineConfig() {
  return fileLoader.fromGit(
    'pipeline', 
    'https://github.com/samsung-cnct/zonar-pipeline.git', 
    'master', 
    'repo-scan-access', 
    ''
  ).getConfig()
}

def loadPipelineSettings() {
  return fileLoader.fromGit(
    'settings', 
    'https://github.com/samsung-cnct/zonar-pipeline.git', 
    'master', 
    'repo-scan-access', 
    ''
  ).getConfig()
}