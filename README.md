# Guardian-Email Client
This is an application I made for SOFT3202 in university.
It uses the Guardian Open Platform API to search news
articles, the list of which can then be sent to an email
via SendGrid's API, or posted to one's personal account
on Reddit via its API.

## Usage
Each of the APIs can run in online or offline modes.
For each, in the online mode, it communicates with the actual API
and gets/sends whatever information (including respecting queries
made). In the offline modes, pre-made data is displayed 
no matter the interaction (e.g. no matter what query
for tags, it will always show the same data).

By default `gradle run` will run all APIs in offline modes.
To run in online modes, you need to specify which
APIs to do so. This can be done via 
`gradle run args="[guardian] [email] [reddit]"`,
where `[guardian]`/`[email]`/`[reddit]` are replaced
with `online` or `offline` depending on what is desired.

Note to use the online modes for each API you need
the necessary keys. See:
- [The Guardian Open Platform](https://open-platform.theguardian.com/)
- [SendGrid](https://sendgrid.com/)
- [Reddit](https://www.reddit.com/prefs/apps) (this application expects usage of a personal use script)

You then need to use the appropriate keys and information
in the following environment variables:
- `INPUT_API_KEY`: API key for The Guardian API
- `SENDGRID_API_KEY`: API key for SendGrid API
- `SENDGRID_API_EMAIL`: Sending email for SendGrid API
- The following are for Reddit features; the application will
run without these, but Reddit functionality won't work
  - `REDDIT_API_CLIENT`: Client ID
  - `REDDIT_API_SECRET`: Client Secret
  - Note the Reddit login you provide in the application
    will have to be associated with these keys!

If any of these are missing (except for the Reddit keys) and
online mode is on for that API, the application will not
run and display an error (I might update this in future to instead
show a dialog to input missing keys, no guarantees though!).

### Testing
If you want to run the tests, use the usual `gradle test`.
No environment variables are required for this.

## Features
- Search news articles by tag and title
- (In online modes) Use cached results
- Make a reading list of articles
- Send the list of articles to an email address
- Post the list of articles to your personal Reddit account
- Most importantly...Dark mode!

## Planned Features
Some planned features for if I will work on this in
the future:
- Make UI elements resize and scale with window size
- Article Viewing in-application
- Make Reddit functionalities work as install app instead of script
- Anything else I may feel like...