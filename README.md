# atomic_broadcast

This repo provides an atomic broadcast architecture implementation in java using Aeron.

The repo is meant to provde the infrastructure for any event driven system and in particular addresses the following common challenges found in low latency financial trading applications:

- A high throughput zero gc sequencer producing an ordered event stream
- An efficient asynchronous journalling mechanism using aeron-archive
- An efficient replication mechanism to follower nodes using aeron-replicate
- An efficient replay merge mechanism using aeron.
       
