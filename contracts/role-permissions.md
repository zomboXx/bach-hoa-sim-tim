# Role and permission matrix

Server roles are authoritative. Frontend visibility is only a usability aid.

| Capability | admin | manager | sales | stock | accountant |
|---|:---:|:---:|:---:|:---:|:---:|
| Read own profile and permitted aggregate state | ✓ | ✓ | ✓ | ✓ | ✓ |
| Read products/categories | ✓ | ✓ | ✓ | ✓ | — |
| Manage products/categories/suppliers/promotions | ✓ | ✓ | — | — | — |
| Read suppliers | ✓ | ✓ | — | ✓ | — |
| Create invoice | ✓ | — | ✓ | — | — |
| Receive stock | ✓ | — | — | ✓ | — |
| Create/approve stock count | ✓ | ✓ | — | — | — |
| Use training session endpoints | ✓ | ✓ | ✓ | ✓ | ✓ |

All denials must occur in `services/api`, even when the web UI hides the corresponding action.
