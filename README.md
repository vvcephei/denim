denim
=========

The fundamental idea here is to provide an http client interface that makes it easy to do what you want
to do 90% of the time.

I took a first crack at it and called it version 0.0. I'm planning to use it in a
few projects and tweak the interface any way I need to work up to version 1.0.
The implementation might be a little funny, but I wanted to not spend too much time on crafting it until I'm sure
it looks the way I want.
That said, I do expect it to work, so let me know if it doesn't.

If you have suggestions, please let me know. I'm all ears.

### Want
* might as well focus on scala. I'd also like a nice java client, but my brevity expectations aren't as high in java

### Don't Want
* no weird dsl syntax. Scala's support for new operators is very powerful, but let's drink responsibly.
* no passing around ```Class[T]``` or ```GenericType[T](){}``` or anything like that. We have ```Manifest```. Let's use it.




Usage
-----
This library is a thinish wrapper around the jersey client and uribuilder.
```scala
import org.vvcephei.RestClient.client

val jsonClient   = client().`type`(MediaType.APPLICATION_JSON_TYPE)
val tumblrClient = jsonClient.segment("http://api.tumblr.com", "v2", "blog")
                             .param("api_key" -> "fuiKNFp9vQFvjLNvx4sUwti4Yb5yGutBN4Xh10LXZhhRKjWlV4")

val scipsyClient = tumblrClient.segment("scipsy.tumblr.com", "info")
val goodClient   = tumblrClient.segment("good.tumblr.com", "info")

val scipsyInfo = scipsyClient.get[Map[String, Object]]()
val goodInfo   = goodClient.get[Map[String, Object]]()

```

You can pass a jersey client in to the factory method, configured as you please:
```scala

val jersey = Client.create()
jersey.setConnectTimeout(10000)
val tenSecondJsonClient = client(jersey)
```
