# One-time setup: Git, GitHub and SonarCloud

Commands are for PowerShell on Windows, run from `C:\code\learning\onboarding-platform`.

## 1. Add the Maven wrapper and build locally

```powershell
mvn -N wrapper:wrapper "-Dmaven=3.9.11"   # creates mvnw, mvnw.cmd and .mvn/wrapper/
.\mvnw.cmd verify                          # must end with BUILD SUCCESS
```

If the build fails on a Spring Boot version, check the latest 4.x release on
https://spring.io/projects/spring-boot and update `<version>` in the root `pom.xml`.

## 2. First commit

```powershell
git init -b main
git add .
git update-index --chmod=+x mvnw    # make the wrapper executable on the Linux CI runners
git commit -m "chore: initial project setup with CI pipeline"
```

## 3. Create the GitHub repository

Either with the GitHub CLI:

```powershell
gh repo create onboarding-platform --public --source=. --push
```

or create an empty **public** repo on github.com (no README, no .gitignore) and:

```powershell
git remote add origin https://github.com/<your-account>/onboarding-platform.git
git push -u origin main
```

Then replace `OWNER` in the badges at the top of `README.md` with your GitHub account name.

Check the **Actions** tab: `CI` and `CodeQL` should run and turn green.

## 4. Repository settings

**Settings → General → Pull Requests**

- Allow **squash merging** only (one commit per PR on `main`, with the PR title as message)
- Enable **Automatically delete head branches**

**Settings → Advanced Security** (or *Code security*)

- Dependabot alerts and Dependabot security updates: on
- Secret scanning and **push protection**: on

**Settings → Rules → Rulesets → New branch ruleset**

- Name `main`, enforcement **Active**, target: default branch
- Restrict deletions, block force pushes, require linear history
- Require a pull request before merging (required approvals: 0, since you work alone)
- Require status checks to pass: `Build & test`, `Analyze (java-kotlin)`
  (checks only show up in the list after they have run once)

## 5. SonarCloud (free for public repos)

1. Sign in at https://sonarcloud.io with GitHub and import the `onboarding-platform` repo.
2. Under **Administration → Analysis Method**, turn **Automatic Analysis off** (CI does the analysis).
3. Create a token (**My Account → Security**).
4. In GitHub, **Settings → Secrets and variables → Actions**:
   - Secret `SONAR_TOKEN` = the token
   - Variable `SONAR_ORGANIZATION` = your SonarCloud organisation key
   - Variable `SONAR_PROJECT_KEY` = the project key shown in SonarCloud
5. Re-run CI: the SonarCloud step now runs and the quality gate appears on PRs.
   Optionally add `SonarCloud Code Analysis` as a required status check.

## 6. Try the workflow

```powershell
git switch -c feat/health-info
# make a small change, e.g. add info.app.description to application.yml
git commit -am "feat: expose application description on /actuator/info"
git push -u origin feat/health-info
gh pr create --fill
```

Watch the checks on the PR, then squash-merge it. That is the loop for every change from now on.
