# asid: an attempt to prove de-centralised identity is possible

*License*: [GPL Affero v3][agpl].

## Background

Back at the beginning of January 2011 I read Aaron Swartz's article
['Squaring the Triangle'][squarezooko]. Which was very interesting. In
September last year, an analyst from Forrester wanted to talk to me
about consumer mobile technology, especially if NFC could be useful
for something other than payments (where it's failing hopelessly.) It
occurred to me that some combination of these two could be the
beginning of a de-centralised system of identity. Except, of course, I
realised that there's no need for a human readable name when it comes
to identity.

A person's smartphone is, effectively, their identity. They trust their
device, they log in and stay logged in on it, and then they never
leave it out of their sight or let other people touch it. So could
something be built that let people's mobile phone become their
identity? Except, I wouldn't want to see this happen if it just meant
Apple (or Google) ended up controlling everything.

At this point, I had the idea that something could exist, maybe. But
no strong picture of what that might look like. After speaking with
[Smári McCarthy][smari] I had a more concrete idea of what could
actually be implemented. And he pointed out that I could build this as
a concept just using a web app, without worrying about the mobile part
just yet.

There are two aims at the core of this project. Firstly, no central
authority should be required to validate all identities (this means
that no single breach should be able to damage existing identities.)
Secondly, identities should be capability based: your identity is your
willingness to answer questions about yourself.

## What does the identity system look like?

On your phone, create a wallet. This would consist of a globally
unique, meaningless identity and a key pair. To this identity you can
then attach key-value pairs of data (such as name, birthday, etc.)

When you physically encounter someone you trust you can use NFC (or
something similar) to exchange identities. Later, if further
information is required, then the wallet can be queried remotely. The
owner can tell who is asking and choose to provide the information, or
not. And the requester can use the identity exchange network to tell
if they trust the information they received.

Exchanging identities means:

1. Signing the other person's identity string with your key pair.
2. Meeting and signing the challenge.
3. Exchanging URIs to access the identity.
4. Exchanging identity information for other people we've already encountered.

### Meeting and signing the challenge

The challenge is a set of keys that must be provided. Every person can
create trust pools that requires sharing certain pieces of
information. For someone to enter a trust pool they must share that
information and have that signed by the person who is welcoming them
into the network. For example, one pool may require name and
email. Another, just email. And a third, name, home address and birth
date. The signed challenge information is used to indicate who
accepted the information as being legitimate.

### Exchanging URIs to access the identity

Ideally, this would point back to software running on the phone. A
centralised service to delegate the request to the phone would be
required, but that would be for routing purposes. It wouldn't
participate in trust, and it would never store information.

### Exchanging identity information

This would consist of the identity string, and the identity URI. And
it would be for everyone you know of who has met the trust pool
challenge, and through which identity you received this
information. If you view a trust pool as a network of identities who
have encountered each other, after this step the two principals now
have the same trust pool networks. Except, that they would also have
the signed data for the people they have encountered directly. This
wouldn't be shared, just to reduce data volume.

## Using the Identities

As an identity consumer, you have the identity string associated with
a physical being standing in front of you and you want to be able to
verify that the person, for example, is over 18. You query the
identity URI for this information, and the owner of the identity
approves the request. You also look through your network for a
connection to this identity that you trust. When you receive the DoB
information, you will also receive verification of who validated it
(those signatures exchanged earlier) if it was part of the challenge
to join this network. Or, if it wasn't part of the challenge, you can
still see who chose to validate the identity and make a trust decision
based on that information.

As you can see, this is not trying to offer iron-clad guarantees about
data integrity. Because that's not actually possible (I feel.) Instead
it's a system for maintaining control over your information, and then
providing enough information to make an informed trust decision.

## Further Work

Some way to untrust someone. This can be done entirely locally by
marking an identity as untrustworthy in a pool. This would break
connections, and cause you to not trust their signatures. It might
also be interesting to exchange this information as well.

Trust pools might actually be a business opportunity. Effectively,
social networks decentralise and move into these trust pools. That's
the feeling I'm getting.

## The PoC

I find this complex enough to think about that I think it actually
needs working code to get a real feel that it's workable. So that's
why I'm writing this app. I suspect there may be some deep flaws, but
I think I'm going to need to actually see code to be sure they're
really there.

## How to run the tests

`lein midje` will run all tests.

`lein midje namespace.*` will run only tests beginning with "namespace.".

`lein midje :autotest` will run all the tests indefinitely. It sets up a
watcher on the code files. If they change, only the relevant tests will be
run again.


[agpl]: http://www.gnu.org/licenses/agpl-3.0.html
[squarezooko]: http://www.aaronsw.com/weblog/squarezooko
