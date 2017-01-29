#!/usr/bin/groovy
def call(commitSha) {  
  return isChartChange(commitSha)
}

def isChartChange(commitSha) {
  def returnStatus = sh(
    returnStatus: true,
    script: """set -eo pipefail

get-merge-commit-changes() {
  merge_commit=\"${commitSha}\"

  # Grab 'Merge: abc1234 def5678' and convert to abc1234..def5678
  child_commit_range=\"\$(git show \"\${merge_commit}\" | grep 'Merge:' | cut -c8- | sed 's/ /../g')\"

  echo \"Returning changes from merge commit '\${merge_commit}' using the commit range: \${child_commit_range}\" >&2

  git diff-tree --no-commit-id --name-only -r \"\${child_commit_range}\"
}

echo $(get-merge-commit-changes) | grep 'charts/' 
    """
  )

  return (returnStatus == 0)
}