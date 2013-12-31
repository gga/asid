Note: In order for asid to make sense, there's a separate [glossary](Glossary.md) of all the objects that exist.

# Creating a connection between two users

There will be one user called the initiator and another called the trustee. The initiator starts the connection, and the trustee must agree. A successfully established connection will be symmetric.

## Initiator Initiates

To begin, the initiator needs two pieces of information.

1. The trustee identity id.
2. The URL for the trustee wallet.

The initiator will `GET` the `introduction` from that the trustee URL. This will contain the following information.

* Trustee identity.
* Trustee public key.
* A signature for an identity packet containing the trustee identity.

The initiator should now verify that the identity it has been provided with matches the identity in the introduction, and the signed identity packet.

Confident they are talking to the trustee they expect to be talking to, the initiator can now proceed.

The initiator will create a `calling-card` to save locally, and then `POST` a `connection-request` to the `letterplate` URL included in the trustee's introduction. The calling card will contain the following.

* A globally unique id for the calling card itself.
* The URL for the trustee wallet.
* The identity of the trustee.

The posted connection request will contain the following.

* `from` &mdash; the identity of the initiator
* `trust.name` &mdash; the name of the trust pool to be joined
* `trust.identity` &mdash; the id of the trust pool
* `trust.challenge` &mdash; an array of keys to provide values for when joining the pool
* `links.self` &mdash; URL to the calling card the initiator has saved
* `links.initiator` &mdash; URL to the initiator

Upon receiving a connection request, the trustee user agent will have to defer to the trustee. The UA will therefore save the connection request. This creates a counterpart for the original calling card. The trustee UA must respond with a `201 Created` with the `Location` header containing the URL for the calling card counterpart. The initiator must save this URL with the calling card.

## Trustee Accepts

Once the trustee has agreed to accept the connection request and join a trust network with the initiator, there are several steps that must be performed. At a high level:

1. Build and submit the challenge response.

   1. *Receive and verify the trustee's challenge response.*
   2. *Build and submit initiator challenge response.*
   
2. Receive and verify the initiator's challenge response.
3. Create trust pool, if required.
4. Send existing trust pool connections to the initiator.

   1. *Receive new trust pool members at the trust pool (may be an empty set).*
   2. *Respond with existing trust pool connections.*
   
5. Receive existing trust pool connections from the initiator.

### Build and submit challenge response

### Receive and verify challenge response

### Create trust pool

### Send trust pool connections

### Receive existing trust pool connections
