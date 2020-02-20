package ws.gmax

import java.security.{KeyPairGenerator, SecureRandom}

package object jwt {
  type Roles = Set[String]

  case class AuthInfo(iss: String, sub: String, authorization: Option[Roles])

  val ROLE_ADMIN = "ROLE_ADMIN"
  val ROLE_READ = "ROLE_READ"
  val ROLE_WRITE = "ROLE_WRITE"

  val allRoles = Set(ROLE_ADMIN, ROLE_READ, ROLE_WRITE)

  val privateKey =
    """
      |-----BEGIN PRIVATE KEY-----
      |MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAKbw7ualY+OXRbdL
      |2jG5oh25virl0Jqp686UTxNNMuOShpzN1Halbptx2RMiJRzykDGss7KjaT+LbhUa
      |zVhRbBZk6Ius1bsaclas8iauGpA9UcEbzsR/pMqijgubN99STGhFgFlZxw6ntL1B
      |ZhIP8SkKbrXmvQpZZLY2NUqUxbRdAgMBAAECgYBQz/8F/fgd20OvWHO2cINO2nR5
      |NajGxgzVgqvIzy0cRvkM/QKlsK2baABKJ9RJcA5nTY/roPk4/pj6dHAFGd01LLrX
      |DncfCS2Rn0RTTiSMOxhZlYcBse1wzb4+E0uUfUfa1JYXm5eY3rumr8o1S+Pdy7ez
      |ABPgeIfzMqEExFBqQQJBAOXh/TGNwgKOw9oM/U9kGU2ugCqrbUyiKFUWzh+LiTH5
      |FU4R/baeAXMFHGgFHQjTm/MvFBhEar/ql1j7s50gYHECQQC56FKMTlgApwXUVwon
      |aip5Tdu+yePfaSg+g04oeRLYXkwpFyi22RCC9mrUUfKMufIXESm4ZwtB57RYGFth
      |cQitAkEA28twb6HPXuyrm9+RjwfxHZH731Bax8u/bmPInuamPY6fbS7Me3+leRjo
      |6RgCg773u9NGjlFUE700ChNWz6P2MQJBAIQXxP+YcwMTqhq0NazHzKIgZjDr9pO5
      |fjTcy14KmQ9QAUF5CR7SoN7NBB8UkwjW3mLxePljjiYn4oZt2BAmZokCQQDPPtLI
      |MrH/Ou2WIkNNMQNiFGrJ1b0odEQdHgXxkV9BX0yrvM6s2LLh3V7/D+MsXjqlD/p1
      |/eu5UHfvOb+4tkXg
      |-----END PRIVATE KEY-----
    """.stripMargin

  val publicKey =
    """
      |-----BEGIN PUBLIC KEY-----
      |MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCm8O7mpWPjl0W3S9oxuaIdub4q
      |5dCaqevOlE8TTTLjkoaczdR2pW6bcdkTIiUc8pAxrLOyo2k/i24VGs1YUWwWZOiL
      |rNW7GnJWrPImrhqQPVHBG87Ef6TKoo4LmzffUkxoRYBZWccOp7S9QWYSD/EpCm61
      |5r0KWWS2NjVKlMW0XQIDAQAB
      |-----END PUBLIC KEY-----
    """.stripMargin

  val begPrivate = "-----BEGIN PRIVATE KEY-----\n"
  val endPrivate = "-----END PRIVATE KEY-----\n"

  val begPublic = "-----BEGIN PUBLIC KEY-----\n"
  val endPublic = "-----END PUBLIC KEY-----\n"

  def generateKeyPair(kpg: String = "RSA",
                      sr: String = "SHA1PRNG",
                      provider: String = "SUN",
                      keySize: Int = 1024) = {
    val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance(kpg)
    val random: SecureRandom = SecureRandom.getInstance(sr, provider)
    keyGen.initialize(keySize, random)
    keyGen.generateKeyPair
  }

  class TokenRoles(enabled: Boolean, roles: Roles) {
    private def hasRole(role: String) = if (enabled) roles.contains(role) else true

    def hasAdminRole = hasRole(ROLE_ADMIN)

    def hasReadRole = hasRole(ROLE_READ)

    def hasWriteRole = hasRole(ROLE_WRITE)
  }

  object TokenRoles {
    def apply(enabled: Boolean, roles: Roles): TokenRoles = new TokenRoles(enabled, roles)
  }
}

