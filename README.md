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

Le catalogue (`fr.lmdp.catalog`) est persisté en base de données via Spring Data JPA. Le schéma initial est géré par **Flyway**, dans un seul fichier : `src/main/resources/db/migration/V1__init.sql`. Hibernate ne fait que valider le schéma (`ddl-auto=validate`).

- **Développement** (profil par défaut) : base **H2** embarquée dans un fichier (`./data/cawl.mv.db`), aucune installation requise.
- **Production** (profil `prod`) : base **PostgreSQL**, configurée dans `application-prod.yml`.

Le même script s'applique aux deux moteurs. Il crée `products` (stock et image binaire `BYTEA` inclus) et `admin_users`, sans aucun produit de démonstration.

### Premier compte administrateur

Après la création des tables, `AdminUserSeeder` crée automatiquement un compte si la table `admin_users` est vide :

- `CAWL_ADMIN_USERNAME` : identifiant, `admin` par défaut.
- `CAWL_ADMIN_PASSWORD` : mot de passe à définir sur Render. Seul son hash BCrypt est enregistré en base.
- Si le mot de passe est absent, un mot de passe temporaire est généré et affiché une seule fois dans les logs. Connexion sur `/admin/login`.

Les démarrages suivants ne recréent pas le compte et ne changent pas son mot de passe. Aucun secret n'est inscrit dans le SQL.

### Bases créées avec les anciennes migrations V1–V6

La consolidation en une seule V1 est prévue pour une **base neuve**. Elle n'est pas compatible avec l'ancien historique Flyway (checksums et versions déjà appliquées).

Sauvegarder les données avant toute remise à zéro. En local, application arrêtée, conserver une copie de `data/cawl.mv.db`, puis utiliser une base H2 neuve. Sur Render, utiliser une base PostgreSQL neuve, ou réinitialiser volontairement la base existante uniquement si ses données peuvent être perdues. La remise à zéro efface produits, photos et comptes ; le compte admin sera recréé au démarrage.

Ne pas supprimer seulement `flyway_schema_history` ni utiliser `repair` comme remplacement d'une migration des données. Si des données doivent être conservées, garder l'ancien historique jusqu'à organiser leur transfert. Aucune base existante n'est automatiquement effacée par cette modification.

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
java -jar target/lmdp-website-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Sur Render, définir `SPRING_PROFILES_ACTIVE=prod`. Au premier démarrage sur une base vide, Flyway applique `V1__init.sql`, puis le compte administrateur est créé. Le catalogue démarre vide.

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

Les tests couvrent les paiements, les images des produits et l'initialisation d'une base vide avec un compte administrateur. Les tests de base utilisent H2 en mémoire, sans modifier `data/cawl.mv.db`. Aucun paiement CAWL distant n'est exécuté.

```bash
mvn clean test
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
