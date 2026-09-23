# CAWL + Spring Boot : exemple minimal

Java 17 · Spring Boot 3.5.16 · Maven · SDK Online Payments 7.0.0.

Démonstration pour **CAWL e-commerce en préproduction**, avec page de paiement hébergée. Le montant est fixé côté serveur à **19,90 EUR** pour la commande `demo-001`. Aucune donnée de carte ne traverse ce backend.

## Démarrer

Prérequis : JDK 17, Maven 3.6.3+ et compte CAWL de test avec les moyens de paiement activés. Les clés API et les clés de webhook sont deux couples distincts, configurés dans le portail marchand CAWL.

Dans un terminal bash, remplacer les valeurs ci-dessous par les identifiants de test (ne pas les committer) :

```bash
export CAWL_PSPID='votre-identifiant-marchand-test'
export CAWL_API_KEY='votre-cle-api-test'
export CAWL_API_SECRET='votre-secret-api-test'
export CAWL_WEBHOOK_KEY='votre-identifiant-cle-webhook'
export CAWL_WEBHOOK_SECRET='votre-secret-webhook'
export CAWL_RETURN_URL='http://localhost:8080/payment-return'
mvn spring-boot:run
```

L'application écoute sur `127.0.0.1:8080`. L'URL API CAWL est volontairement fixée à `https://payment.preprod.cawl-solutions.fr/` dans `CawlConfig`.

## Effectuer un paiement de test

1. Configurer une URL HTTPS publique de webhook dans le portail CAWL, acheminée vers `POST /api/payments/cawl/webhook` sur cette application. Un tunnel de développement peut transférer cette seule route vers localhost. CAWL ne peut pas joindre directement votre localhost. Ne pas exposer les routes de commande de cette démo sans authentification.
2. Activer les événements de paiement nécessaires, notamment la capture, et utiliser l'envoi de test du portail. `payment.test` signé est accepté sans modifier de commande.
3. Créer une session :

```bash
curl -X POST http://localhost:8080/api/orders/demo-001/checkout
```

Réponse contenant `hostedCheckoutId` et `redirectUrl`. Ouvrir la valeur de `redirectUrl` dans le navigateur, puis utiliser les données de carte de test indiquées dans la documentation CAWL. Pour un frontend de même origine :

```javascript
const response = await fetch('/api/orders/demo-001/checkout', { method: 'POST' });
if (!response.ok) throw new Error('Impossible de créer le paiement');
const checkout = await response.json();
window.location.assign(checkout.redirectUrl);
```

4. Après paiement, consulter :

```bash
curl http://localhost:8080/api/orders/demo-001/payment-status
```

Seul un événement signé avec statut `CAPTURED`, la bonne référence, le bon compte, le bon montant et la bonne devise fait passer la commande à `PAID`. Une autorisation ou `CAPTURE_REQUESTED` ne suffit pas. Le retour navigateur n'effectue aucune validation de commande. Pour la carte, la requête utilise le mode `SALE`.

## Catalogue produits & base de données

Le catalogue (`fr.example.cawl.catalog`) est persisté en base de données via Spring Data JPA. Le schéma est entièrement géré par **Flyway** (`src/main/resources/db/migration`) : Hibernate n'a que le droit de valider le schéma (`ddl-auto=validate`), jamais de le modifier.

- **Développement** (profil par défaut) : base **H2** embarquée dans un fichier (`./data/cawl.mv.db`), aucune installation requise.
- **Production** (profil `prod`) : base **PostgreSQL**, configurée dans `application-prod.yml`.

Les mêmes scripts SQL de migration s'appliquent aux deux moteurs (types standards : `VARCHAR`, `DECIMAL`, `BOOLEAN`, `TIMESTAMP`).

### Démarrer en production avec PostgreSQL

1. Créer une base PostgreSQL (ex. `cawl`) et un utilisateur dédié.
2. Définir les variables d'environnement :

```bash
export DB_HOST='mon-serveur-postgres'
export DB_PORT='5432'
export DB_NAME='cawl'
export DB_USER='cawl_app'
export DB_PASSWORD='mot-de-passe-securise'
```

3. Lancer l'application avec le profil `prod` activé :

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
# ou, avec le jar packagé :
java -jar target/cawl-demo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Au démarrage, Flyway se connecte à PostgreSQL et applique automatiquement les migrations manquantes (création de la table `products`, jeu de données de démonstration, etc.).

### API du catalogue

| Méthode | Route | Description |
|---|---|---|
| `GET` | `/api/produits` | Liste des produits (lecture seule, utilisée par le panier public) |
| `GET` | `/api/produits/{id}` | Détail d'un produit |
| `POST` | `/api/admin/produits` | Créer un produit (back-office, à protéger avant mise en production) |
| `PUT` | `/api/admin/produits/{id}` | Modifier un produit |
| `DELETE` | `/api/admin/produits/{id}` | Supprimer un produit |

Pour ajouter une évolution de schéma, créer un nouveau fichier `V{n}__description.sql` dans `db/migration` : Flyway l'appliquera automatiquement au prochain démarrage, sur H2 comme sur PostgreSQL.

## Organisation


| Fichier | Responsabilité |
|---|---|
| `CawlConfig.java` | Client SDK, compte marchand et vérificateur de signatures |
| `PaymentService.java` | Création de session, référence de commande, contrôles et état |
| `PaymentController.java` | Routes HTTP, réception du corps brut du webhook |
| `PaymentTest.java` | Tests locaux sans identifiants CAWL ni appels réseau |

Une seconde demande de checkout renvoie la même session. Après un échec ambigu de création (par exemple un timeout), l'exemple bloque une nouvelle création : vérifier d'abord le portail CAWL. Il ne prétend pas garantir l'idempotence distante de `CreateHostedCheckout` avec un simple header.

Les webhooks passent par le vérificateur officiel du SDK avant traitement. Les événements déjà traités sont ignorés. Une notification retardée ne fait pas repasser une commande payée à un état en attente. Les événements pour d'autres commandes sont ignorés. Les identifiants de paiement CAWL peuvent évoluer au cours des opérations : la corrélation utilise ici la référence marchand créée par le serveur.

## Tests

Validation locale effectuée : compilation réussie et 6 tests réussis. Aucun paiement CAWL distant exécuté, faute d’identifiants de compte.

```bash
mvn test
mvn package
```

Les tests contrôlent le montant serveur, la réutilisation du checkout, les notifications répétées et retardées, l'autorisation seule, le mauvais montant, le mauvais marchand, ainsi que la signature sur les octets exacts et le rejet d'un corps modifié.

## Limites explicites de cette démo

- Stockage en mémoire, une seule commande, une seule instance. Redémarrer perd sessions et déduplication. Une nouvelle référence est alors créée ; ne pas redémarrer pour contourner un paiement ambigu sans vérifier CAWL.
- Les routes de commande n'ont pas d'authentification : elles restent accessibles uniquement en local. Dans votre application, authentifier le client et vérifier qu'il possède la commande avant création ou lecture.
- Remplacer l'état mémoire par des tables commandes, tentatives de paiement et événements, avec contraintes uniques et transactions. Persister le webhook avant le `2xx`, puis effectuer les traitements longs en arrière-plan. Prévoir une outbox pour la livraison et un rapprochement via l'API en cas de webhook manquant.
- Cette démo conserve « paiement initial confirmé ». Elle ne gère pas les remboursements, litiges, captures partielles, nouvelles tentatives après refus ni l'expiration d'un checkout. Modéliser ces opérations séparément avant production.
- Si votre projet utilise Spring Security avec sessions, conserver CSRF sur les routes navigateur ; exempter uniquement le webhook CAWL, qui reste protégé par sa signature.
- Le chemin réel CAWL doit encore être testé avec votre compte : réussite, refus, authentification 3DS, abandon, webhook retardé et retour navigateur sans webhook.

## Sources officielles

- [SDK Java CAWL](https://docs.ecommerce.cawl-solutions.fr/en/integration/how-to-integrate/server-sdks/java)
- [Hosted Checkout](https://docs.ecommerce.cawl-solutions.fr/en/integration/basic-integration-methods/hosted-checkout-page)
- [Webhooks](https://docs.ecommerce.cawl-solutions.fr/en/integration/api-developer-guide/webhooks)
- [Cartes et scénarios de test](https://docs.ecommerce.cawl-solutions.fr/en/integration/test-cases)
- [SDK officiel sur GitHub](https://github.com/Online-Payments/sdk-java)
