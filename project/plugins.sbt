addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.22")

resolvers += Resolver.bintrayIvyRepo("rtimush", "sbt-plugin-snapshots")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.4.0")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")