#Zuul Server Change Logs

### 2013-10-01 13:57

Added password change capabalities. This includes:

- A post method at '/password/change' that takes in three parameters:
	- old_password
	- new_password
- Supports changing password only when.
	- The old password provided is correct.
	- The new password is valid.
	- The new password is the same as the old password.
- Authorisation required for this release.
- A database migration is required for this release.