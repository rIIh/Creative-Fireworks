package de.erdbeerbaerlp.creativefirework.blocks.tileEntity;

import static de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter.CREATIVE;
import static de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter.DELAY;
import static de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter.FLIGHT;
import static de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter.MODE;
import static de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter.SHAPE;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import de.erdbeerbaerlp.creativefirework.MainClass;
import de.erdbeerbaerlp.creativefirework.MainClass.Shape;
import de.erdbeerbaerlp.creativefirework.blocks.BlockFireworkShooter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemFireworkRocket;
import net.minecraft.item.ItemFireworkStar;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;

public class TileEntityShooter extends TileEntity implements ITickable{
	VoxelShape COLLECTION_AREA_SHAPE = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 32.0D, 16.0D);



	private int paper = 5;
	private int gunpowder = 5;


	public TileEntityShooter() {
		super(MainClass.TE_FWSHOOTER);
	}
	@Override
	public NBTTagCompound write(NBTTagCompound compound) {
		compound.setInt("paper", paper);
		compound.setInt("gunpowder", gunpowder);
		return super.write(compound);
	}
	@Override
	public void read(NBTTagCompound compound) {
		paper = compound.getInt("paper");
		gunpowder = compound.getInt("gunpowder");
		super.read(compound);
	}

	@Override
	@Nullable
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		System.out.println("[DEBUG]:Server sent tile sync packet");
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		write(nbtTagCompound);

		return new SPacketUpdateTileEntity(this.pos, 0, nbtTagCompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
		System.out.println("[DEBUG]:Client recived tile sync packet");
		read(pkt.getNbtCompound());
	}
	@Override
	public NBTTagCompound getUpdateTag()
	{
		System.out.println("GetUpdate");
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		write(nbtTagCompound);
		return nbtTagCompound;
	}

	@Override
	public void handleUpdateTag(NBTTagCompound tag)
	{
		this.read(tag);
	}




	// -----  Fireworking!   -----


	int i=0;
	Random r = new Random();
	@Override
	public void tick() {
		
		BlockPos pos = this.getPos();
		IBlockState state = world.getBlockState(pos);
		if(!(state.getBlock() instanceof BlockFireworkShooter)) return;
		if(world.isRemote) {
			i=0;
			return;
		}
		if(!state.get(CREATIVE))
			for(EntityItem item : getCaptureItems()) {
				int itemCount = item.getItem().getCount();
				if(item.getItem().getItem() == Items.PAPER) {
					setPaper(5*itemCount+getPaper());
					item.remove();
				}else if(item.getItem().getItem() == Items.GUNPOWDER) {
					setGunpowder(5*itemCount+getGunpowder());
					item.remove();
				}else if(item.getItem().getItem() instanceof ItemFireworkRocket) {
					setPaper(5*itemCount+getPaper());
					setGunpowder(5*itemCount+getGunpowder());
					item.remove();
				}else if(item.getItem().getItem() instanceof ItemFireworkStar) {
					setGunpowder(5*itemCount+getGunpowder());
					item.remove();
				}else if(item.getItem().getItem() == Blocks.SUGAR_CANE.asItem()) {
					setPaper(5*itemCount+getPaper());
					item.remove();
				}else if(item.getItem().getItem() == Items.BOOK || item.getItem().getItem() == Items.WRITTEN_BOOK || item.getItem().getItem() == Items.WRITABLE_BOOK) {
					setPaper(15*itemCount+getPaper());
					item.setItem(new ItemStack(Items.LEATHER, itemCount));
				}else if(item.getItem().getItem() instanceof ItemEnchantedBook) {
					setPaper(30*itemCount+getPaper());
					item.remove();
				}else if(item.getItem().getItem() == Blocks.TNT.asItem()) {
					setGunpowder(25*itemCount+getGunpowder());
					item.setItem(new ItemStack(Blocks.SAND, 4*itemCount));
				}
			}
		if((getGunpowder() <= 0 || getPaper() <= 0) && !state.get(CREATIVE)) return;
		switch(state.get(MODE)) {
		case REDSTONE:
			if(!world.isBlockPowered(pos)) {
				i=0;
				return;
			}
			break;
		case REDSTONE_INVERTED:
			if(world.isBlockPowered(pos)) {
				i=0;
				return;
			}
			break;
		case ALWAYS_OFF:
			i=0;
			return;
		case ALWAYS_ON:
			break;
		case AUTOMATIC_NIGHT:
			if(checkUp(world, pos) || !((world.getSkylightSubtracted()) > 5)) {
				i=0;
				return;
			}

			break;

		case AUTOMATIC_DAY:
			if(checkUp(world, pos) || !((world.getSkylightSubtracted()) < 5)) {
				i=0;
				return;
			}
			break;
		default:
			return;
		}
		boolean canFire = world.isAirBlock(new BlockPos(pos.getX(), pos.getY()+1, pos.getZ()));

		if(i > (state.get(DELAY)*20)) {
			System.out.println("Fire!");
			final int[] colors = new int[r.nextInt(8)];
			for(int i = 0; i<colors.length;i++) {
				colors[i] = r.nextInt(99999999);
			}
			final int[] fade = new int[r.nextInt(8)];
			for(int i = 0; i<fade.length;i++) {
				fade[i] = r.nextInt(99999999);
			}
			int hole = r.nextInt(3);
			int hole2 = r.nextInt(3);
			double addx = 0;
			double addz = 0;
			if(hole == 0) addx = 0.2;
			if(hole == 1) addx = 0.5;
			if(hole == 2) addx = 0.75;

			if(hole2 == 0) addz = 0.2;
			if(hole2 == 1) addz = 0.5;
			if(hole2 == 2) addz = 0.75;
			if(canFire) {
				if(!state.get(CREATIVE)) {
					setGunpowder(getGunpowder()-1);
					setPaper(getPaper()-1);
				}
				Entity e = new EntityFireworkRocket(world, pos.getX()+addx, pos.getY(), pos.getZ()+addz, getFirework(state.get(FLIGHT), state.get(SHAPE), r.nextBoolean(), r.nextBoolean(), colors, fade));
				world.spawnEntity(e);
			}else {
				Entity e = new EntityFireworkRocket(world, pos.getX()+addx, pos.getY(), pos.getZ()+addz, getFirework(-99, state.get(SHAPE), r.nextBoolean(), r.nextBoolean(), colors, fade));
				world.spawnEntity(e);
				world.createExplosion(e, pos.getX(), pos.getY(), pos.getZ(), 2f, true);
				i=0;
				return;
			}
			i=0;
		}
		i++;
	}
	private boolean checkUp(World world, BlockPos pos) {
		for(int i=1;i<=50;i++) {
			if(!world.isAirBlock(new BlockPos(pos.getX(), pos.getY()+i, pos.getZ()))) return true;
		}
		return false;
	}
	private ItemStack getFirework(int flight, Shape shape, boolean trail, boolean flicker, int[] colors, int[] fadeColors) {
		NBTTagCompound nbt = new NBTTagCompound();
		NBTTagCompound fw = new NBTTagCompound();
		NBTTagList explosion = new NBTTagList();
		NBTTagCompound l = new NBTTagCompound();
		l.setInt("Flicker", flicker ? 1:0);
		l.setInt("Trail", trail ? 1:0);
		l.setInt("Type", (shape == Shape.RANDOM)?new Random().nextInt(Shape.values().length-2)+1:shape.getID()+1);
		l.setTag("Colors", new NBTTagIntArray(colors));
		l.setTag("FadeColors", new NBTTagIntArray(fadeColors));
		explosion.add(l);
		fw.setTag("Explosions", explosion);
		fw.setInt("Flight", flight);
		nbt.setTag("Fireworks", fw);
		ItemStack i = new ItemStack(Items.FIREWORK_ROCKET);
		i.setTag(nbt);
		return i;
	}
	/**
	 * Copied from TileEntityHopper and modified to work here
	 */
	public List<EntityItem> getCaptureItems() {
		return COLLECTION_AREA_SHAPE.toBoundingBoxList().stream().flatMap((p_200110_1_) -> {
			return this.getWorld().getEntitiesWithinAABB(EntityItem.class, p_200110_1_.offset(this.getPos().getX() - 0.5D, this.getPos().getY() - 0.5D, this.getPos().getZ() - 0.5D), EntitySelectors.IS_ALIVE).stream();
		}).collect(Collectors.toList());
	}
	public int getGunpowder() {
		// TODO Auto-generated method stub
		return this.gunpowder;
	}
	public int getPaper() {
		return this.paper;
	}
	public void setPaper(int paper) {
		this.paper = paper;
		markDirty();
	}
	public void setGunpowder(int gunpowder) {
		this.gunpowder = gunpowder;
		markDirty();
	}

}
