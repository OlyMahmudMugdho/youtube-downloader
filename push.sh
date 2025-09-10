git tag -d v1
git push origin --delete v1
git add .
git commit -m "update"
git tag v1
git push origin v1