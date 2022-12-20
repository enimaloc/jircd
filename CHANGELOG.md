# Changelog

## 1.0.0 (2022-12-20)


### ⚠ BREAKING CHANGES

* root package changed to `com.github.enimaloc` to `fr.enimaloc`
* Method `JIRCD#infos()` renamed to `JIRCD#info()`
* package and class has moved

### Features

* **commands:** add AWAY commands ([63660dc](https://www.github.com/enimaloc/jircd/commit/63660dcf6795770bda77ea7c5ce5133281a9f091))
* **commands:** add HELP command ([3bfcf77](https://www.github.com/enimaloc/jircd/commit/3bfcf770dda73f796ca94d59a7555728af821d6f))
* **commands:** add KICK command ([cf5145b](https://www.github.com/enimaloc/jircd/commit/cf5145b08475ebd257fad6e9b2fe39ccec105090))
* **commands:** add LINKS command ([103d075](https://www.github.com/enimaloc/jircd/commit/103d075b7f39423928a5dc70aa60e8908d2af79d))
* **commands:** add LUSER command ([d092643](https://www.github.com/enimaloc/jircd/commit/d0926439170703f35ed376f67b891391da297286))
* **commands:** Add Miscellaneous Messages Commands ([c58bb0c](https://www.github.com/enimaloc/jircd/commit/c58bb0c1fde1d16c240a81fb25cb27f27f50c8e9))
* **commands:** Add Optional Messages Commands ([25a0fc4](https://www.github.com/enimaloc/jircd/commit/25a0fc4de3c9cc08133e9d5943cdaf47de2be5b6))
* **commands:** add REHASH command ([ff5529f](https://www.github.com/enimaloc/jircd/commit/ff5529f2570f0f7767e1b207c1692cfd13226b60))
* **commands:** add RESTART command ([888d195](https://www.github.com/enimaloc/jircd/commit/888d1950283dac0e8d3fa884be2179c414334bbe))
* **commands:** Add Sending Messages Commands ([feafd0a](https://www.github.com/enimaloc/jircd/commit/feafd0a467cff85bfa04dd41342a6b66b5a666e6))
* **commands:** add undocumented PingCommand ([631edac](https://www.github.com/enimaloc/jircd/commit/631edac5cfad32d0bd60d623bd834a7973d4c132))
* **commands:** add WALLOPS command ([7cad07a](https://www.github.com/enimaloc/jircd/commit/7cad07ab0d2810a831371cc8a8e62b44b52cef48))
* **commands:** add WHO command ([4afd432](https://www.github.com/enimaloc/jircd/commit/4afd4321fa5940632bac2faa81f7a2f37dfc78e5))
* **commands:** add WHOIS command ([c9b9d36](https://www.github.com/enimaloc/jircd/commit/c9b9d36496b2d4c335c0ce05d1cdd3d7e11b0af0))
* **commands:** add WHOWAS command ([12cfcc1](https://www.github.com/enimaloc/jircd/commit/12cfcc1a51653eee13889421af7b59ab41e08f8e))
* **commands:** Added channel operations ([ed1be41](https://www.github.com/enimaloc/jircd/commit/ed1be41f01481f007ba83e352e58f1af84a13642))
* **commands:** Added connection commands ([d37e513](https://www.github.com/enimaloc/jircd/commit/d37e51341e1e9193b7c7343c3c19ce9f44c023bb))
* **commands:** partially add SQUIT commands ([8db767e](https://www.github.com/enimaloc/jircd/commit/8db767e9e7e5ca6e1b718d715306c224cc0fa7e8))
* **commands:** USER with no trailing ([bbcd0d6](https://www.github.com/enimaloc/jircd/commit/bbcd0d6ea994facb75d613584fc23f0457b0e1d6))
* **connection:** Added missing motd ([43a0cfd](https://www.github.com/enimaloc/jircd/commit/43a0cfd639f908ff8f81a5a720137ba42c5002ea))
* **server queries and commands:** Add Server Queries And Command ([3636485](https://www.github.com/enimaloc/jircd/commit/363648516b2323af0b3b952f3f558612328deb2a))
* **settings:** add settings file ([19c2813](https://www.github.com/enimaloc/jircd/commit/19c28136959126e99bf393f440d06d61233889c6))


### Bug Fixes

* **005 RPL_ISUPPORT:** Extra-space at the end of tokens list removed ([8d12fc0](https://www.github.com/enimaloc/jircd/commit/8d12fc06fbb05934e03cad8501545ceab49a4284))
* **attribute:** exclude transient field from map function ([037fcfc](https://www.github.com/enimaloc/jircd/commit/037fcfc9528e95991bec694c03130bca563b102e))
* cannot edit final field ([cbac70a](https://www.github.com/enimaloc/jircd/commit/cbac70acac39d10b6db4e6b917a507947ec737b8))
* **channelModes:** add default List in case of null (like in test) to avoid NPE ([75979be](https://www.github.com/enimaloc/jircd/commit/75979be22509b43dcf1f9409c5f4f3fcb2d8e698))
* **channelModes:** correct formatting of modes arguments ([b0ad559](https://www.github.com/enimaloc/jircd/commit/b0ad55961452a4ae3e13b2396e35401ac9d0abc1))
* **commands:** builder append `]` char for each user in channel ([2917791](https://www.github.com/enimaloc/jircd/commit/2917791e8737585afa992f76faca10d0478bfed4))
* **commands:** incorrect uptime sent in STATS U ([d26a15a](https://www.github.com/enimaloc/jircd/commit/d26a15a4f44ece8f5c70e916fc9008b75cf6718d))
* **commands:** nullpointerexception when a command need more parameters ([6934e2f](https://www.github.com/enimaloc/jircd/commit/6934e2f348a2e88be8ef36a41ccc5763e91adba1))
* **commands:** send RPL_REHASHING before reloading configuration ([6a33b02](https://www.github.com/enimaloc/jircd/commit/6a33b02a2434d63ec410de6a505b3ed7cfd0c2dd))
* **commit:** Added missing class to 43a0cf ([882d6ce](https://www.github.com/enimaloc/jircd/commit/882d6ceca4acc91460aa13fcecfffc527b24937d))
* **configuration:** null pointer exception thrown because if file not exist is not created ([3914f3d](https://www.github.com/enimaloc/jircd/commit/3914f3d080d5d1ebcfc3925f05004028166e6179))
* **cpu:** Fix 100% core usage ([5de84f7](https://www.github.com/enimaloc/jircd/commit/5de84f778221c093ccf52bd57633647f21d4164d)), closes [#1](https://www.github.com/enimaloc/jircd/issues/1)
* **list:** issue when putting a non-existent channel ([c22bb75](https://www.github.com/enimaloc/jircd/commit/c22bb75b74f875fd9449cdd7acab2ace7ca3b27d))
* **list:** mask are now used here ([c48e65b](https://www.github.com/enimaloc/jircd/commit/c48e65baf288edf153eac08082cfef92cdbb52cf))
* **list:** support more than 3 digits numbers ([60594fd](https://www.github.com/enimaloc/jircd/commit/60594fda03f8765f4795c7e3f6fcafb4a2e9a3c3))
* **mask:** incorrectly escaped char ([ab0f479](https://www.github.com/enimaloc/jircd/commit/ab0f4792c0272a5eb836577c101d3b423b57e73f))
* **mask:** mask not correctly escaped ([49ea394](https://www.github.com/enimaloc/jircd/commit/49ea394211990f85b1a223c26820c2bfaf6bfadb))
* **message:** Finishing 59f49c and code cleanup ([f219ca6](https://www.github.com/enimaloc/jircd/commit/f219ca65e637570e0fedbf6849c79f95a9c385d6))
* **message:** Remaking message part because RPL was not properly format ([59f49cb](https://www.github.com/enimaloc/jircd/commit/59f49cbf095c665286db3d0c74b9ede9c21c723e))
* **message:** Trailing was not priority when running the command ([4ac8d33](https://www.github.com/enimaloc/jircd/commit/4ac8d33902a801dd4bea2b21175b13a9e8fdc495))
* **nick:** add username check with unsafenick and safenet ([efa9f9e](https://www.github.com/enimaloc/jircd/commit/efa9f9efc578b4eae5aaa48d1c0e5ad4f249ec06))
* removed forgotten System.out.println ([ddcf96a](https://www.github.com/enimaloc/jircd/commit/ddcf96af92baf59a918ba78b09bf82293db2e52c))
* **test:** disabling whole restartTest for now ([08f2882](https://www.github.com/enimaloc/jircd/commit/08f2882b7a94e0cb4c514a5d9beefa4e12634468))
* **test:** fix env ([9d816e9](https://www.github.com/enimaloc/jircd/commit/9d816e99bc6d28ba6f036a6e223bed4e1e0e55a3))
* **test:** fix invalid assertion ([bf32bc3](https://www.github.com/enimaloc/jircd/commit/bf32bc339660aff7af0b9b19eb030923e109dcdf))
* **test:** fix invalid tests due to recent change ([b8e23e6](https://www.github.com/enimaloc/jircd/commit/b8e23e6490f1ee516e0acc519559732896cff27e))
* **test:** generate incorrect nick in `incorrectNickTest` can fail the test ([5f1a6cf](https://www.github.com/enimaloc/jircd/commit/5f1a6cfc29e0b22532de2b5c7d10e501581a79df))
* **test:** improve awaitMessage() method ([47a3d2a](https://www.github.com/enimaloc/jircd/commit/47a3d2aef7865316c1d035e8f3420fb36493cd91))
* **test:** improve awaitMessage() method ([f40b46f](https://www.github.com/enimaloc/jircd/commit/f40b46f482fbb027b4349bfa79a8b0894e768657))
* **test:** improve waiting time in userTest() ([3ad876d](https://www.github.com/enimaloc/jircd/commit/3ad876d1de2c1aa441c3f78be31f8b233a044aed))
* **test:** need to create another config file on REHASH test ([eb9a473](https://www.github.com/enimaloc/jircd/commit/eb9a4735c4b9b594ca3066668233b2cf576a9bd7))
* **test:** oper(int index) await confirmation message ([38548ea](https://www.github.com/enimaloc/jircd/commit/38548ea90b675e8cf1cc269febca4f19e434bbe0))
* **test:** put a temp fix until i find a solution ([39a552a](https://www.github.com/enimaloc/jircd/commit/39a552acf07093cdb52afc7a9232fbbfe26a6af1))
* **tests:** fix forgotten command in STATS ([d39d2c4](https://www.github.com/enimaloc/jircd/commit/d39d2c4db23d297a4b09006a4d9631e2956c31df))
* **tests:** fix unknown connection after handling LUSER ([fff4102](https://www.github.com/enimaloc/jircd/commit/fff410280a4fc97f37eae9476bc878e11fb343c0))
* **tests:** make fix global ([2820167](https://www.github.com/enimaloc/jircd/commit/2820167bb70f9de23d3758c22fe01a4b57f82ba6))
* **tests:** try fix timeout when waiting a thread interrupt ([8090217](https://www.github.com/enimaloc/jircd/commit/80902176d4377e5146f101aec8015ab1f77b0d37))
* **test:** termination message fail test with disconnect message in socket ([fcf8dd7](https://www.github.com/enimaloc/jircd/commit/fcf8dd7196eb0ecfd5b8108d092718b3e1bde836))
* typo ([ceb2acf](https://www.github.com/enimaloc/jircd/commit/ceb2acf8b91156c1bcf9146516878002c613cef1))
* **user:** fix oper mode not set ([300d40b](https://www.github.com/enimaloc/jircd/commit/300d40b8590977dde41aee245651034911871df9))
* **user:** forgotten import of Constant ? ([53fd900](https://www.github.com/enimaloc/jircd/commit/53fd900e68ac373aa9eb40cfdc493a4c34991f0a))


### Performance Improvements

* avoid array creation in `Mask` class ([579e5bb](https://www.github.com/enimaloc/jircd/commit/579e5bb6f9329a1dd7d91be8b2e197192183b6ee))
* using Paths instead of File ([dd73a63](https://www.github.com/enimaloc/jircd/commit/dd73a63b3e9a95782001dfad0ba5dd5aa28a33e0))


### Code Refactoring

* change root package ([4cfd545](https://www.github.com/enimaloc/jircd/commit/4cfd545cdf4e7f98bb23c059c5c00d48b6625f15))
* refactor for better code arrangement ([ecc5047](https://www.github.com/enimaloc/jircd/commit/ecc50475aa580e6b5aa26f3290c48d3d9431b89c))
