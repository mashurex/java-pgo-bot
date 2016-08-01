package com.ashurex.pokemon.bot.action;
import POGOProtos.Data.Battle.BattlePokemonInfoOuterClass;
import POGOProtos.Data.PokemonDataOuterClass;
import POGOProtos.Networking.Requests.Messages.StartGymBattleMessageOuterClass;
import POGOProtos.Networking.Requests.RequestTypeOuterClass;
import POGOProtos.Networking.Responses.AttackGymResponseOuterClass;
import POGOProtos.Networking.Responses.StartGymBattleResponseOuterClass;
import com.google.protobuf.InvalidProtocolBufferException;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Battle;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.PokeBank;
import com.pokegoapi.api.player.PlayerProfile;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;
import com.pokegoapi.main.ServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: Mustafa Ashurex
 * Created: 7/27/16
 */
public class GymFighter
{
    private static final Logger LOG = LoggerFactory.getLogger(GymFighter.class);

    public void startBattle(PokemonGo api, Pokemon[] team, Gym gym)
    throws LoginFailedException, RemoteServerException
    {
        StartGymBattleResponseOuterClass.StartGymBattleResponse battleResponse;
        StartGymBattleMessageOuterClass.StartGymBattleMessage.Builder builder = StartGymBattleMessageOuterClass.StartGymBattleMessage.newBuilder();

        for (int i = 0; i < team.length; i++) {
            builder.addAttackingPokemonIds(team[i].getId());
        }


        List<PokemonDataOuterClass.PokemonData> defenders = gym.getDefendingPokemon();
        builder.setGymId(gym.getId());
        builder.setPlayerLongitude(api.getLongitude());
        builder.setPlayerLatitude(api.getLatitude());
        builder.setDefendingPokemonId(defenders.get(0).getId()); // may need to be sorted

        ServerRequest serverRequest = new ServerRequest(
            RequestTypeOuterClass.RequestType.START_GYM_BATTLE, builder.build());
        api.getRequestHandler().sendServerRequests(serverRequest);


        try {
            battleResponse = StartGymBattleResponseOuterClass.StartGymBattleResponse.parseFrom(serverRequest.getData());
        } catch (InvalidProtocolBufferException e) {
            throw new RemoteServerException();
        }

        // need to send blank action
        // this.sendBlankAction();

/*
        for (BattleActionOuterClass.BattleAction action : battleResponse.getBattleLog().getBattleActionsList()) {
            gymIndex.add(action.getTargetIndex());
        }

        return battleResponse.getResult();
*/
    }

    public static void fight(PlayerProfile profile, Gym gym, PokeBank pokeBank)
    {
        try
        {
            LOG.debug("Attempting to fight at " + gym.getName());
            if (!gym.getEnabled() || !gym.isAttackable())
            {
                LOG.debug("Gym is disabled or not attackable!");
                return;
            }
/*
            if(profile.getTeam().getValue() == gym.getOwnedByTeam().getNumber())
            {
                LOG.info("Not training against same team yet.");
                return;
            }
*/
            // TODO: Figure out good fight matchups
            List<PokemonDataOuterClass.PokemonData> defenders = gym.getDefendingPokemon();
            Pokemon[] team = getFightingTeam(defenders, pokeBank, defenders.size());

            Battle battle = gym.battle(team);
            StartGymBattleResponseOuterClass.StartGymBattleResponse.Result result = battle.start();
            Sleeper.sleep();

            if(result == StartGymBattleResponseOuterClass.StartGymBattleResponse.Result.ERROR_GYM_BATTLE_LOCKOUT)
            {
                Sleeper.longSleep();
                result = battle.start();
            }

            if(result != StartGymBattleResponseOuterClass.StartGymBattleResponse.Result.SUCCESS)
            {
                LOG.warn("Could not start gym battle: " + result);
                return;
            }

            boolean abortBattle = false;
            BattlePokemonInfoOuterClass.BattlePokemonInfo attacker = null;
            BattlePokemonInfoOuterClass.BattlePokemonInfo defender = null;

            while (!battle.isConcluded() && !abortBattle)
            {
                Sleeper.sleep();
                AttackGymResponseOuterClass.AttackGymResponse r = battle.attack(5);
                if(r.getResult() == AttackGymResponseOuterClass.AttackGymResponse.Result.SUCCESS)
                {
                    attacker = r.getActiveAttacker();
                    defender = r.getActiveDefender();
                    PokemonDataOuterClass.PokemonData aData = attacker.getPokemonData();
                    PokemonDataOuterClass.PokemonData dData = defender.getPokemonData();

                    int dStam = defender.getPokemonData().getStaminaMax();
                    int aStam = attacker.getPokemonData().getStaminaMax();

                    LOG.info(String.format("ATTACK %s %d/%d [%d] vs. %s %d/%d [%d]",
                        r.getActiveAttacker().getPokemonData().getPokemonId(),
                        attacker.getCurrentHealth(),
                        aStam,
                        attacker.getCurrentEnergy(),

                        defender.getPokemonData().getPokemonId(),
                        defender.getCurrentHealth(), dStam,
                        defender.getCurrentEnergy()));
                }
                else
                {
                    LOG.warn("Unsuccessful Result: " + r.getResult());
                    abortBattle = true;
                }
            }

            Sleeper.shortSleep();
            LOG.info("Battle outcome: " + battle.getOutcome());
        }
        catch(Exception ex)
        {
            LOG.error(ex.getMessage());
            ex.printStackTrace();
        }
    }


    public static Pokemon[] getFightingTeam(List<PokemonDataOuterClass.PokemonData> defenders, PokeBank pokeBank, int size)
    {
        List<Pokemon> list = pokeBank.getPokemons()
                     .stream()
                     .filter(p -> { return !p.isInjured() && !p.isFainted(); })
                     .sorted((Pokemon a, Pokemon b) -> Integer.compare(b.getCp(), a.getCp()))
                     .collect(Collectors.toList()).subList(0, 6);
        // return list.toArray(new Pokemon[list.size()]);

        Pokemon[] val = new Pokemon[6];
        for(int i = 0; i < val.length; i++)
        {
            val[i] = list.get(i);
        }

        return val;
    }

}
