/* Copyright (c) 2021-2023 DeflatedPickle under the MIT license */

package com.deflatedpickle.squidfriends.mixin;

import com.deflatedpickle.squidfriends.api.IBreedable;
import com.deflatedpickle.squidfriends.api.IDoesAge;
import com.deflatedpickle.squidfriends.mojank.FollowParentSquidGoal;
import com.deflatedpickle.squidfriends.mojank.SquidMateGoal;
import java.util.UUID;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"UnusedMixin", "WrongEntityDataParameterClass"})
@Mixin(SquidEntity.class)
public abstract class MixinSquidEntity extends WaterCreatureEntity implements IBreedable, IDoesAge {
  private static final TrackedData<Boolean> CHILD;
  protected int breedingAge;
  protected int forcedAge;
  protected int happyTicksRemaining;
  private int loveTicks;
  @Nullable private UUID lovingPlayer;

  static {
    CHILD = DataTracker.registerData(SquidEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
  }

  protected MixinSquidEntity(EntityType<? extends WaterCreatureEntity> entityType, World world) {
    super(entityType, world);
  }

  public EntityData initialize(
      ServerWorldAccess world,
      LocalDifficulty difficulty,
      SpawnReason spawnReason,
      @Nullable EntityData entityData,
      @Nullable NbtCompound entityNbt) {
    if (entityData == null) {
      entityData = new PassiveEntity.PassiveData(true);
    }

    PassiveEntity.PassiveData passiveData = (PassiveEntity.PassiveData) entityData;
    if (passiveData.canSpawnBaby()
        && passiveData.getSpawnedCount() > 0
        && world.getRandom().nextFloat() <= passiveData.getBabyChance()) {
      this.setBreedingAge(-24000);
    }

    passiveData.countSpawned();
    return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
  }

  @ModifyArg(
      method = "initGoals",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/entity/ai/goal/GoalSelector;add(ILnet/minecraft/entity/ai/goal/Goal;)V"),
      index = 0)
  public int priorityIncrement(int priority) {
    return priority + 2;
  }

  @Inject(method = "initGoals", at = @At("TAIL"))
  public void onInitGoals(CallbackInfo ci) {
    this.goalSelector.add(0, new SquidMateGoal((SquidEntity) (Object) this, 1.0));
    this.goalSelector.add(1, new FollowParentSquidGoal((SquidEntity) (Object) this, 1.1));
  }

  @Override
  public void onGrowUp() {}

  @Override
  public boolean isBaby() {
    return this.getBreedingAge() < 0;
  }

  @Override
  public boolean isAdult() {
    return this.getBreedingAge() >= 0;
  }

  @Override
  public void setBaby(boolean baby) {
    this.setBreedingAge(baby ? -24000 : 0);
  }

  private static int m_eyazmlyb(int i) {
    return (int) ((float) (i / 20) * 0.1F);
  }

  public SquidEntity createChild(ServerWorld world, SquidEntity entity) {
    return EntityType.SQUID.create(world);
  }

  protected void initDataTracker() {
    super.initDataTracker();
    this.dataTracker.startTracking(CHILD, false);
  }

  public int getBreedingAge() {
    if (this.world.isClient) {
      return this.dataTracker.get(CHILD) ? -1 : 1;
    } else {
      return this.breedingAge;
    }
  }

  public void setBreedingAge(int age) {
    int i = this.getBreedingAge();
    this.breedingAge = age;
    if (i < 0 && age >= 0 || i >= 0 && age < 0) {
      this.dataTracker.set(CHILD, age < 0);
      this.onGrowUp();
    }
  }

  public void growUp(int age, boolean overGrow) {
    int i = this.getBreedingAge();
    int j = i;
    i += age * 20;
    if (i > 0) {
      i = 0;
    }

    int k = i - j;
    this.setBreedingAge(i);
    if (overGrow) {
      this.forcedAge += k;
      if (this.happyTicksRemaining == 0) {
        this.happyTicksRemaining = 40;
      }
    }

    if (this.getBreedingAge() == 0) {
      this.setBreedingAge(this.forcedAge);
    }
  }

  public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt);
    nbt.putInt("Age", this.getBreedingAge());
    nbt.putInt("ForcedAge", this.forcedAge);

    nbt.putInt("InLove", this.loveTicks);
    if (this.lovingPlayer != null) {
      nbt.putUuid("LoveCause", this.lovingPlayer);
    }
  }

  public void readCustomDataFromNbt(NbtCompound nbt) {
    super.readCustomDataFromNbt(nbt);
    this.setBreedingAge(nbt.getInt("Age"));
    this.forcedAge = nbt.getInt("ForcedAge");

    this.loveTicks = nbt.getInt("InLove");
    this.lovingPlayer = nbt.containsUuid("LoveCause") ? nbt.getUuid("LoveCause") : null;
  }

  public void onTrackedDataSet(TrackedData<?> data) {
    if (CHILD.equals(data)) {
      this.calculateDimensions();
    }

    super.onTrackedDataSet(data);
  }

  protected void mobTick() {
    if (this.getBreedingAge() != 0) {
      this.loveTicks = 0;
    }

    super.mobTick();
  }

  @Inject(method = "tickMovement", at = @At("TAIL"))
  public void onTickMovement(CallbackInfo ci) {
    if (this.world.isClient) {
      if (this.happyTicksRemaining > 0) {
        if (this.happyTicksRemaining % 4 == 0) {
          this.world.addParticle(
              ParticleTypes.HAPPY_VILLAGER,
              this.getParticleX(1.0),
              this.getRandomBodyY() + 0.5,
              this.getParticleZ(1.0),
              0.0,
              0.0,
              0.0);
        }

        --this.happyTicksRemaining;
      }
    } else if (this.isAlive()) {
      int i = this.getBreedingAge();
      if (i < 0) {
        this.setBreedingAge(++i);
      } else if (i > 0) {
        this.setBreedingAge(--i);
      }
    }
  }

  public boolean isBreedingItem(ItemStack stack) {
    return stack.isOf(Items.COD) || stack.isOf(Items.SALMON) || stack.isOf(Items.TROPICAL_FISH);
  }

  @Override
  public ActionResult interactMob(PlayerEntity player, Hand hand) {
    ItemStack itemStack = player.getStackInHand(hand);
    if (this.isBreedingItem(itemStack)) {
      int i = this.getBreedingAge();
      if (!this.world.isClient && i == 0 && this.canEat()) {
        this.eat(player, hand, itemStack);
        this.lovePlayer(player);
        return ActionResult.SUCCESS;
      }

      if (this.isBaby()) {
        this.eat(player, hand, itemStack);
        this.growUp(m_eyazmlyb(-i), true);
        return ActionResult.success(this.world.isClient);
      }

      if (this.world.isClient) {
        return ActionResult.CONSUME;
      }
    }

    return super.interactMob(player, hand);
  }

  protected void eat(PlayerEntity player, Hand hand, ItemStack stack) {
    if (!player.getAbilities().creativeMode) {
      stack.decrement(1);
    }
  }

  public boolean canEat() {
    return this.loveTicks <= 0;
  }

  public void lovePlayer(@Nullable PlayerEntity player) {
    this.loveTicks = 600;
    if (player != null) {
      this.lovingPlayer = player.getUuid();
    }

    this.world.sendEntityStatus(this, EntityStatuses.ADD_BREEDING_PARTICLES);
  }

  @Nullable
  public ServerPlayerEntity getLovingPlayer() {
    if (this.lovingPlayer == null) {
      return null;
    } else {
      PlayerEntity playerEntity = this.world.getPlayerByUuid(this.lovingPlayer);
      return playerEntity instanceof ServerPlayerEntity ? (ServerPlayerEntity) playerEntity : null;
    }
  }

  @Override
  public boolean isInLove() {
    return this.loveTicks > 0;
  }

  public void resetLoveTicks() {
    this.loveTicks = 0;
  }

  @Override
  public boolean canBreedWith(@NotNull WaterCreatureEntity other) {
    if (other == this) {
      return false;
    } else if (other.getClass() != this.getClass()) {
      return false;
    } else {
      return this.isInLove() && ((MixinSquidEntity) other).isInLove();
    }
  }

  @Override
  public void breed(@NotNull ServerWorld world, SquidEntity other) {
    SquidEntity passiveEntity = this.createChild(world, other);
    if (passiveEntity != null) {
      ServerPlayerEntity serverPlayerEntity = this.getLovingPlayer();
      if (serverPlayerEntity == null
          && ((MixinSquidEntity) (Object) other).getLovingPlayer() != null) {
        serverPlayerEntity = ((MixinSquidEntity) (Object) other).getLovingPlayer();
      }

      if (serverPlayerEntity != null) {
        serverPlayerEntity.incrementStat(Stats.ANIMALS_BRED);
        // Criteria.BRED_ANIMALS.trigger(serverPlayerEntity, this, other, passiveEntity);
      }

      this.setBreedingAge(6000);
      ((MixinSquidEntity) (Object) other).setBreedingAge(6000);
      this.resetLoveTicks();
      ((MixinSquidEntity) (Object) other).resetLoveTicks();
      passiveEntity.setBaby(true);
      passiveEntity.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
      world.spawnEntityAndPassengers(passiveEntity);
      world.sendEntityStatus(this, EntityStatuses.ADD_BREEDING_PARTICLES);
      if (world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
        world.spawnEntity(
            new ExperienceOrbEntity(
                world, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
      }
    }
  }

  @Override
  public void handleStatus(byte status) {
    if (status == EntityStatuses.ADD_BREEDING_PARTICLES) {
      for (int i = 0; i < 7; ++i) {
        double d = this.random.nextGaussian() * 0.02;
        double e = this.random.nextGaussian() * 0.02;
        double f = this.random.nextGaussian() * 0.02;
        this.world.addParticle(
            ParticleTypes.HEART,
            this.getParticleX(1.0),
            this.getRandomBodyY() + 0.5,
            this.getParticleZ(1.0),
            d,
            e,
            f);
      }
    } else {
      super.handleStatus(status);
    }
  }
}
