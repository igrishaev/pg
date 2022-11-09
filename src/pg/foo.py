
import base64
import hashlib
import hmac
import secrets
import stringprep
import unicodedata


def bytes_xor(a, b):
    return bytes(a_i ^ b_i for a_i, b_i in zip(a, b))


def scram_sha_256_generate_salted_password(password, salt, iterations, digest):

    """This follows the "Hi" algorithm specified in RFC5802"""
    # first, need to normalize the password using PostgreSQL-flavored SASLprep

    normalized_password = password

    # convert the password to a binary string - UTF8 is safe for SASL (though there are SASLPrep rules)
    p = normalized_password.encode("utf8")

    # generate a salt
    # self.salt = secrets.token_bytes(salt_length)

    # the initial signature is the salt with a terminator of a 32-bit string ending in 1
    ui = hmac.new(p, salt + b'\x00\x00\x00\x01', digest)

    # grab the initial digest

    u = ui.digest()

    # for X number of iterations, recompute the HMAC signature against the password
    # and the latest iteration of the hash, and XOR it with the previous version

    for x in range(iterations - 1):
        ui = hmac.new(p, ui.digest(), hashlib.sha256)

        # this is a fancy way of XORing two byte strings together
        u = bytes_xor(u, ui.digest())

    return u


print(scram_sha_256_generate_salted_password("secret", bytes(base64.b64decode("MXf1hERKrJWAQSlcYSRe6A==")), 4096, hashlib.sha256).hex())
