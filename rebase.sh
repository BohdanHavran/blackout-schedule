#!/bin/sh

currentBranch=$(git rev-parse --abbrev-ref HEAD)

if [[ -n "$(git status -s)" ]]; then
  echo "You have uncommitted changes. Please commit or stash them before rebasing."
  git status -s
  exit 1
fi

echo "Rebasing $currentBranch to master"
echo ""
echo "Actualize master"
git checkout master -q
git pull origin master -q
echo ""


echo "Actualize $currentBranch"
git checkout $currentBranch -q
git pull origin $currentBranch -q
echo ""

backupbrn="$currentBranch-backup-$(date +%Y%m%d%H%M%S)"
echo "Backup $currentBranch to $backupbrn"
git branch $backupbrn -q

echo "Press Enter to continue..."

read yes

git rebase -i master
