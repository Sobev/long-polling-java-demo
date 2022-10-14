package com.sobev.longpolling;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class LongpollingApplicationTests {

  @Test
  void contextLoads() {
  }

  static boolean search(int[] nums, int target) {
    int lo = 0, hi = nums.length - 1;
    while (lo <= hi) {
      while (lo < hi && nums[lo] == nums[lo + 1])
        ++lo;
      while (lo < hi && nums[hi] == nums[hi - 1])
        --hi;
      int mid = lo + (hi - lo) / 2;
      if (nums[mid] == target) {
        return true;
      }
      // mid落在左段
      else if (nums[mid] > nums[lo]) {
        if (target > nums[lo] && target < nums[mid]) {
          hi = mid - 1;
        } else {
          lo = mid + 1;
        }
      }
      // 落在右段
      else {
        if (target > nums[mid] && target < nums[hi]) {
          lo = mid + 1;
        } else {
          hi = mid - 1;
        }
      }
    }
    return false;
  }
  public static int coinChange(int[] coins, int amount) {
    int[] dp = new int[amount + 1];
    Arrays.fill(dp, Integer.MAX_VALUE);
    dp[0] = 0;
    for (int i = 1; i < amount + 1; i++) {
      int s = Integer.MAX_VALUE;
      for (int j = 0; j < coins.length; j++) {
        int idx = i - coins[j];
        if (idx < 0 || dp[idx] == Integer.MAX_VALUE)
          continue;
        s = Math.min(dp[idx], s);
      }
      if(s != Integer.MAX_VALUE)
        dp[i] = s + 1;
    }
    return dp[amount] == Integer.MAX_VALUE?-1:dp[amount];
  }
  public static List<List<Integer>> threeSum(int[] nums) {
    List<List<Integer>> res = new ArrayList<>();
    if(nums.length < 3)
      return res;
    Arrays.sort(nums);
    for (int i = 0; i < nums.length; i++) {
      if(i > 0 && nums[i-1] == nums[i])
        continue;
      int l = i+1, r = nums.length - 1;
      while (l < r){
        if(nums[i] + nums[l] + nums[r] > 0){
          r--;
        }
        else if(nums[i] + nums[l] + nums[r] < 0){
          l++;
        }
        else{
          res.add(Arrays.asList(nums[i],nums[l],nums[r]));
          while (l < r && nums[l] == nums[l+1]) l++;
          while (r > l && nums[r] == nums[r - 1]) r--;
          l++;
          r--;
        }
      }
    }
    return res;
  }
  static class CommonMapper<S extends Mapper<T>,T>  {

  }
  interface Mapper<T>{
    T getT();
  }
  static class MapperImpl<T> implements Mapper<T>{
    T t;
    public MapperImpl(T t){
      this.t = t;
    }

    @Override
    public T getT() {
      return this.t;
    }
  }

  public static void main(String[] args) {
    Mapper<Long> m = new MapperImpl<>(123L);
    System.out.println(m.getT());
    CommonMapper<MapperImpl<Long>,Long> commonMapper = new CommonMapper<>();
  }

}
