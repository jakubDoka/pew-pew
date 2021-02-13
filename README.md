# Weapon booster

Pewpew adds extra content to game, it allows you to define new weapons that can be activated when having items on hand with
any unit. Every unit can have unique weapon for each item it uses. Plugin is light but powerful and getting
it running requires same procedure as any other plugin. Let's go trough config syntax, as that's how you add content to game.
Now `important thing to note` any unit that has items in it will be able to shoot with a weapon, if it is defined in a config. 

## config

```json
{
    "def":{
        "copperGun":{
            "bullet": "standardCopper",
            "inaccuracy": 2,
            "damageMultiplier":1.0,
            "reload": 0.4,
            "bulletsPerShot": 2,
            "ammoMultiplier": 4,
            "itemsPerScoop": 1
        },
        "scepterShotgun":{
            "bullet": "scepter-1",
            "inaccuracy": 10,
            "damageMultiplier":0.5,
            "reload": 2.0,
            "bulletsPerShot": 40,
            "ammoMultiplier": 1,
            "itemsPerScoop": 10
        },
        "bulletBoost": {
          "bullet": "self-1",
          "inaccuracy": 0,
          "damageMultiplier":1.0,
          "reload": 0.2,
          "bulletsPerShot": 1,
          "ammoMultiplier": 1,
          "itemsPerScoop": 1
        }
    },
    "links":{
        "alpha":{
            "copper": "copperGun"
        },
        "beta":{
            "copper": "copperGun"
        },
        "gamma":{
            "copper": "copperGun",
            "phaseFabric": "bulletBoost"
        },
        "scepter":{
            "surgeAlloy": "scepterShotgun"
        }
    }
}
```
In property `def` you can define a weapon and then in links you can link it with specific unit and item, current config adds
`copperGun` to all units spawned by core thus units that players are starting with. Now lets go over the weapon statistics. 

First is a `bullet` that specifies what kind of bullet this gun uses, in this case it's a `standardCopper` which is a most 
basic bullet that `alfa` shoots. You can view list of available bullets with commandline command `pew-content bullets`.
Bullets are not tested so if you find one that crashes the game, write an issue about it. Bullet can also be chosen differently.
Some units have unique weapons, and to use them in your weapon you have to use `unitName-weaponIndex` syntax. to view what units
offer use `pew-content unit-weapons` command. You can also use `self-weaponIndex` to specify that weapon should use whatever
unit using it is using.

Then there is `inaccuracy`, if you set it to 180, weapon will shoot in random direction.

Next we have `damageMultiplier`, it does what is says just set it to 10000, and you will see.

`reload` sets delay between shots, so 1.0 is 1 shot per second and 0.0 is 60 shots per second or more or less (depends on fps).

`bulletsPerShot` sets how many bullets gun will fire per one reload, so combination of inaccuracy and big bps will 
create shotgun.

`ammoMultiplier` is ammo multiplier(how unexpected), when weapon consumes an item/s they get transformed into ammo, thus 
multiplier specifies how many times you can shoot per scoop.

`itemsPerScoop` specifies how match items is needed to create one ammo. This allows for expensive ammo (one rail shot
consumes 30 surge).

Other property is `links`, there you specify conditions that has to be met for ammo to activate. You are controlling
alfa and have copper on hand, you will shoot additional projectiles. you can view available list of items and units along
with their exact names you have to use wia `pew-content <units/items>`.

## contribution

Plugin is small, so it's easy to learn how it works, if you feel like it needs an additional feature write an issue first
and then after approval make a pull request.