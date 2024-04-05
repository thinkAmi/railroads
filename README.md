# railroads

![Build](https://github.com/thinkAmi/railroads/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/24076-railroads.svg)](https://plugins.jetbrains.com/plugin/24076-railroads)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24076-railroads.svg)](https://plugins.jetbrains.com/plugin/24076-railroads)

<!-- Plugin description -->

Railroads is a plugin for RubyMine that lists rails routes results and transitions to files.

There was a great plugin called [Railways](https://plugins.jetbrains.com/plugin/7110-railways) that I loved very much. However, since IDE version 2023.3, it is no longer available for installation, so I created this plugin.

## Features

- Displays routes of Ruby on Rails application/engine in a separate "Railroads" panel
- The listed path and name can be copied from the context menu
- Provides quick routes filtering by route path, controller/action or route name

<!-- Plugin description end -->

## Unsupported Features

- IDE versions earlier than `2023.2.x` .
  - if you want to use that version, please use the Railways plugin.
  - for ease of feature comparison with Railways, 2023.2.x is supported at this time. However, if Railroads finds it difficult to support 2023.2.x in the future, it may no longer do so.
- Old format output from rails routes
  - for example, the format of the following test data from Railways
    - https://github.com/basgren/railways/blob/master/test/data/parserTest_1.txt
  - because there is no environment that can output the old format

## TODOs

- Run rails routes when ToolWindow is opened.
- Caching of rails routes results.
- Customize environment, task names, etc.
- Navigation in the code editor.
- Highlight when narrowing down the path.
- Add test code.


## Screenshot

![2024_0403_railroads](https://github.com/thinkAmi/railroads/assets/1299734/2bfb5126-60dd-45f6-816e-3627bc96b904)


## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "railroads"</kbd> >
  <kbd>Install</kbd>
  
- Manually:

  Download the [latest release](https://github.com/thinkAmi/railroads/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## IntelliJ IDEA Ultimate settings for development

- Open IntelliJ IDEA.
- Select `File > New > Project from Version Control` .
- Set `URL` is this repository, and Click `Clone` .
- Open `Project Structure` and Select `Platform Settings > SDKs` .
- Set JDK path.
  - example: Windows + RubyMine's JBR: `C:\Users\<UserName>\AppData\Local\JetBrains\Toolbox\apps\RubyMine\ch-0\233.14808.14\jbr`
- Select `Project Settings > Project` .
- Set the following values, and click `OK` .
  - Name: `railroads`
  - SDK: Select the JDK configured above
  - Language Level: `SDK default`
- Rename `local.properties.examples` to `local.properties` .
- Open `local.properties` and Set `ideDir` .
- Select `Run > Run Plugin`, and The IDE with `Railroads` plugin will start.

## License

see LICENSE file.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
