# Security

**ToTrackIt is designed to run inside a trusted network.** By default the API is unauthenticated — anyone who can reach it can create, complete, and delete processes. Do **not** expose it directly to the public internet.

Deployment options, from simplest to strongest:

1. **Private network only (default).** Run ToTrackIt behind your VPN/firewall and rely on network-level access control.
2. **Static API key.** Set the `TOTRACKIT_API_KEY` environment variable and every `/processes` request must carry a matching `X-API-KEY` header. Health, metrics, and API docs endpoints stay open. This is a single shared key for the whole deployment — suitable for one team, not a user-management system.
3. **Reverse proxy.** Terminate TLS and add your own auth (basic auth, OIDC proxy, etc.) in front of ToTrackIt.

Multi-tenant namespaces, per-user API keys, and SSO are planned for a future managed/enterprise offering and are intentionally not part of the open-source core.
